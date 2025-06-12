package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleOrderGridDTO;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleOrderService {

    private final StockDetailsRepository stockRepository;
    private final MddpStockRepository mddpStockRepository;
    private final ManufacturerOrderRepository manufacturerOrderRepository;
    private final VehicleOrderDetailsRepository orderRepository;
    private final VehicleVariantRepository variantRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final HistoryService historyService;
    private final OrderIdGeneratorService orderIdGeneratorService;

    @Transactional
    public OrderResponse checkAndBlockStock(OrderRequest orderRequest) {
        // Validate request fields
        if (orderRequest.getModelName() == null || orderRequest.getVariant() == null || orderRequest.getColour() == null ||
                orderRequest.getTransmissionType() == null || orderRequest.getFuelType() == null) {
            log.warn("Invalid request for customerOrderId: {}. Missing required fields.", orderRequest.getCustomerOrderId());
            return placeManufacturerOrder(orderRequest);
        }

        // Step 1: Query stock by modelName and vehicleVariantId
        VehicleVariant variant = variantRepository.findById(orderRequest.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found: " + orderRequest.getVehicleVariantId()));
        List<StockDetails> stocks = stockRepository.findByModelNameAndVehicleVariantIdAndStockStatus(
                orderRequest.getModelName(), variant, StockStatus.AVAILABLE);

        if (stocks.isEmpty()) {
            log.info("No available stock for modelName: {}, vehicleVariantId: {}. Placing manufacturer order.",
                    orderRequest.getModelName(), orderRequest.getVehicleVariantId());
            return placeManufacturerOrder(orderRequest);
        }

        // Step 2: Filter stocks in specified order
        StockDetails stock = stocks.stream()
                .filter(s -> orderRequest.getVariant().equalsIgnoreCase(s.getVariant())) // Check variant
                .filter(s -> orderRequest.getColour().equals(s.getColour())) // Check colour
                .filter(s -> orderRequest.getTransmissionType().equals(s.getTransmissionType())) // Check transmissionType
                .filter(s -> orderRequest.getFuelType().equals(s.getFuelType())) // Check fuelType
                .filter(s -> s.getQuantity() >= orderRequest.getQuantity()) // Check quantity
                .findFirst()
                .orElse(null);

        if (stock == null) {
            log.info("No matching stock found for modelName: {}, vehicleVariantId: {}, variant: {}. Placing manufacturer order.",
                    orderRequest.getModelName(), orderRequest.getVehicleVariantId(), orderRequest.getVariant());
            return placeManufacturerOrder(orderRequest);
        }

        // Block the stock
        stock.setQuantity(stock.getQuantity() - orderRequest.getQuantity());
        if (stock.getQuantity() == 0) {
            stock.setStockStatus(StockStatus.DEPLETED);
        }
        stock = stockRepository.save(stock);
        if (stock.getStockId() == null) {
            log.error("Stock ID is null after saving StockDetails for VIN: {}", stock.getVinNumber());
            throw new IllegalStateException("Stock ID is null after saving StockDetails");
        }
        log.info("Stock ID: {} blocked for VIN: {}, modelName: {}, variant: {}",
                stock.getStockId(), stock.getVinNumber(), stock.getModelName(), stock.getVariant());

        historyService.saveStockHistory(stock, "Stock Blocked for Order: " + orderRequest.getCustomerOrderId());

        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.BLOCKED);
        return response;
    }

    @Transactional
    public OrderResponse checkAndReserveMddpStock(OrderRequest orderRequest) {
        // Validate request fields
        if (orderRequest.getModelName() == null || orderRequest.getVariant() == null || orderRequest.getColour() == null ||
                orderRequest.getTransmissionType() == null || orderRequest.getFuelType() == null) {
            log.warn("Invalid request for customerOrderId: {}. Missing required fields.", orderRequest.getCustomerOrderId());
            return placeManufacturerOrder(orderRequest);
        }

        // Step 1: Validate modelName and vehicleVariantId
        VehicleVariant vehicleVariant = variantRepository.findById(orderRequest.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + orderRequest.getVehicleVariantId()));
        VehicleModel vehicleModel = vehicleModelRepository.findById(orderRequest.getVehicleModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + orderRequest.getVehicleModelId()));

        // Query MDDP stock
        Optional<MddpStock> mddpStockOptional = mddpStockRepository.findByVehicleVariantIdAndStockStatus(
                vehicleVariant, StockStatus.AVAILABLE);

        if (mddpStockOptional.isEmpty()) {
            log.info("No MDDP stock available for modelName: {}, vehicleVariantId: {}. Placing manufacturer order.",
                    orderRequest.getModelName(), orderRequest.getVehicleVariantId());
            return placeManufacturerOrder(orderRequest);
        }

        MddpStock mddpStock = mddpStockOptional.get();
        // Step 2: Validate attributes in order
        if (!orderRequest.getModelName().equalsIgnoreCase(mddpStock.getModelName())) {
            log.info("MDDP stock modelName: {} does not match requested modelName: {}. Placing manufacturer order.",
                    mddpStock.getModelName(), orderRequest.getModelName());
            return placeManufacturerOrder(orderRequest);
        }
        if (!orderRequest.getVariant().equalsIgnoreCase(mddpStock.getVariant())) {
            log.info("MDDP stock variant: {} does not match requested variant: {}. Placing manufacturer order.",
                    mddpStock.getVariant(), orderRequest.getVariant());
            return placeManufacturerOrder(orderRequest);
        }
        if (!orderRequest.getColour().equals(mddpStock.getColour())) {
            log.info("MDDP stock colour: {} does not match requested colour: {}. Placing manufacturer order.",
                    mddpStock.getColour(), orderRequest.getColour());
            return placeManufacturerOrder(orderRequest);
        }
        if (!orderRequest.getTransmissionType().equals(mddpStock.getTransmissionType())) {
            log.info("MDDP stock transmissionType: {} does not match requested transmissionType: {}. Placing manufacturer order.",
                    mddpStock.getTransmissionType(), orderRequest.getTransmissionType());
            return placeManufacturerOrder(orderRequest);
        }
        if (!orderRequest.getFuelType().equals(mddpStock.getFuelType())) {
            log.info("MDDP stock fuelType: {} does not match requested fuelType: {}. Placing manufacturer order.",
                    mddpStock.getFuelType(), orderRequest.getFuelType());
            return placeManufacturerOrder(orderRequest);
        }
        if (mddpStock.getQuantity() < orderRequest.getQuantity()) {
            log.info("MDDP stock quantity: {} is less than requested quantity: {}. Placing manufacturer order.",
                    mddpStock.getQuantity(), orderRequest.getQuantity());
            return placeManufacturerOrder(orderRequest);
        }

        // Reserve MDDP stock and create StockDetails
        StockDetails newStock = new StockDetails();
        newStock.setVehicleVariantId(vehicleVariant);
        newStock.setVehicleModelId(vehicleModel);
        newStock.setColour(orderRequest.getColour());
        newStock.setFuelType(orderRequest.getFuelType());
        newStock.setTransmissionType(orderRequest.getTransmissionType());
        newStock.setVariant(orderRequest.getVariant());
        newStock.setQuantity(orderRequest.getQuantity());
        newStock.setStockStatus(StockStatus.AVAILABLE);
        newStock.setCreatedAt(LocalDateTime.now());
        newStock.setModelName(orderRequest.getModelName());
        stockRepository.save(newStock);

        mddpStock.setQuantity(mddpStock.getQuantity() - orderRequest.getQuantity());
        if (mddpStock.getQuantity() == 0) {
            mddpStock.setStockStatus(StockStatus.DEPLETED);
        }
        mddpStockRepository.save(mddpStock);
        historyService.saveStockHistory(newStock, "Stock Transferred from MDDP for Order: " + orderRequest.getCustomerOrderId());

        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.BLOCKED);
        return response;
    }

    @Transactional
    public OrderResponse placeManufacturerOrder(OrderRequest orderRequest) {
        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.PENDING);
        log.info("Placed manufacturer order for customerOrderId: {}, variant: {}", orderRequest.getCustomerOrderId(), orderRequest.getVariant());
        return response;
    }

    @Transactional
    public OrderResponse confirmOrder(OrderResponse orderResponse) {
        log.info("Confirming order for customer: {}, customerOrderId: {}", orderResponse.getCustomerName(), orderResponse.getCustomerOrderId());

        if (orderResponse.getCustomerOrderId() == null) {
            log.error("CustomerOrderId is null for customer: {}", orderResponse.getCustomerName());
            throw new IllegalArgumentException("CustomerOrderId is required to confirm order");
        }

        VehicleOrderDetails orderDetails = orderRepository.findByCustomerOrderId(orderResponse.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with customerOrderId: " + orderResponse.getCustomerOrderId()));

        if (orderDetails.getOrderStatus() != OrderStatus.BLOCKED) {
            log.error("Order with customerOrderId: {} must be in BLOCKED status to confirm, current status: {}", orderResponse.getCustomerOrderId(), orderDetails.getOrderStatus());
            throw new IllegalStateException("Order must be in BLOCKED status to confirm, current status: " + orderDetails.getOrderStatus());
        }

        orderDetails.setOrderStatus(OrderStatus.CONFIRMED);
        orderDetails = orderRepository.save(orderDetails);
        log.info("Saved VehicleOrderDetails with customerOrderId: {}", orderDetails.getCustomerOrderId());

        return mapToOrderResponseFromDetails(orderDetails);
    }

    public OrderResponse notifyCustomerWithTentativeDelivery(OrderRequest orderRequest) {
        OrderResponse orderResponse = mapToOrderResponse(orderRequest);
        VehicleOrderDetails orderDetails = mapToOrderDetails(orderResponse);
        orderDetails.setOrderStatus(OrderStatus.NOTIFIED);
        orderDetails = orderRepository.save(orderDetails);
        orderResponse.setOrderStatus(OrderStatus.NOTIFIED);
        return orderResponse;
    }

    @Transactional
    public OrderResponse cancelOrder(String customerOrderId) {
        VehicleOrderDetails orderDetails = orderRepository.findByCustomerOrderId(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found with customerOrderId: " + customerOrderId));

        if (orderDetails.getOrderStatus() == OrderStatus.COMPLETED || orderDetails.getOrderStatus() == OrderStatus.CANCELED) {
            log.error("Order with customerOrderId: {} cannot be canceled. Current status: {}", customerOrderId, orderDetails.getOrderStatus());
            throw new IllegalStateException("Order cannot be canceled, current status: " + orderDetails.getOrderStatus());
        }

        VehicleVariant variant = orderDetails.getVehicleVariantId();
        VehicleModel model = orderDetails.getVehicleModelId();
        // Query stock by modelName and vehicleVariantId
        List<StockDetails> stocks = stockRepository.findByModelNameAndVehicleVariantIdAndStockStatus(
                orderDetails.getModelName(), variant, StockStatus.AVAILABLE);

        StockDetails stock = stocks.stream()
                .filter(s -> orderDetails.getVariant() != null && orderDetails.getVariant().equalsIgnoreCase(s.getVariant())) // Check variant
                .filter(s -> orderDetails.getColour() != null && orderDetails.getColour().equals(s.getColour())) // Check colour
                .filter(s -> orderDetails.getTransmissionType() != null && orderDetails.getTransmissionType().equals(s.getTransmissionType())) // Check transmissionType
                .filter(s -> orderDetails.getFuelType() != null && orderDetails.getFuelType().equals(s.getFuelType())) // Check fuelType
                .findFirst()
                .orElse(null);

        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + orderDetails.getQuantity());
            stock.setStockStatus(StockStatus.AVAILABLE);
            stockRepository.save(stock);
            historyService.saveStockHistory(stock, "Stock Restored for Canceled Order: " + customerOrderId);
            log.info("Restored stock ID: {} for canceled order: {}", stock.getStockId(), customerOrderId);
        } else {
            log.warn("No matching stock found to restore for canceled order: {}. Creating new stock.", customerOrderId);
            StockDetails newStock = new StockDetails();
            newStock.setVehicleVariantId(variant);
            newStock.setVehicleModelId(model);
            newStock.setColour(orderDetails.getColour());
            newStock.setFuelType(orderDetails.getFuelType());
            newStock.setTransmissionType(orderDetails.getTransmissionType());
            newStock.setVariant(orderDetails.getVariant());
            newStock.setQuantity(orderDetails.getQuantity());
            newStock.setStockStatus(StockStatus.AVAILABLE);
            newStock.setCreatedAt(LocalDateTime.now());
            newStock.setModelName(orderDetails.getModelName());
            stockRepository.save(newStock);
            historyService.saveStockHistory(newStock, "Stock Created for Canceled Order: " + customerOrderId);
        }

        historyService.saveOrderHistory(orderDetails, "system", OrderStatus.CANCELED);
        orderDetails.setOrderStatus(OrderStatus.CANCELED);
        orderRepository.save(orderDetails);

        log.info("Canceled order with customerOrderId: {}", customerOrderId);
        return mapToOrderResponseFromDetails(orderDetails);
    }

    public OrderResponse mapToOrderResponse(OrderRequest request) {
        if (request.getCustomerOrderId() == null) {
            log.error("CustomerOrderId is null in OrderRequest for customer: {}", request.getCustomerName());
            return new OrderResponse(null, OrderStatus.PENDING);
        }

        VehicleVariant variant = variantRepository.findById(request.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found: " + request.getVehicleVariantId()));

        OrderResponse response = new OrderResponse();
        response.setCustomerOrderId(request.getCustomerOrderId());
        response.setVehicleModelId(request.getVehicleModelId());
        response.setVehicleVariantId(request.getVehicleVariantId());
        response.setCustomerName(request.getCustomerName());
        response.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "");
        response.setEmail(request.getEmail() != null ? request.getEmail() : "");
        response.setPermanentAddress(request.getPermanentAddress() != null ? request.getPermanentAddress() : "");
        response.setCurrentAddress(request.getCurrentAddress() != null ? request.getCurrentAddress() : "");
        response.setAadharNo(request.getAadharNo() != null ? request.getAadharNo() : "");
        response.setPanNo(request.getPanNo() != null ? request.getPanNo() : "");
        response.setModelName(request.getModelName());
        response.setFuelType(request.getFuelType());
        response.setColour(request.getColour());
        response.setTransmissionType(request.getTransmissionType());
        response.setVariant(request.getVariant());
        response.setQuantity(request.getQuantity());
        response.setPaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "");
        return response;
    }

    private VehicleOrderDetails mapToOrderDetails(OrderResponse response) {
        VehicleModel vehicleModel = vehicleModelRepository.findById(response.getVehicleModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + response.getVehicleModelId()));
        VehicleVariant vehicleVariant = variantRepository.findById(response.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + response.getVehicleVariantId()));

        VehicleOrderDetails orderDetails = new VehicleOrderDetails();
        orderDetails.setCustomerOrderId(response.getCustomerOrderId() != null ? response.getCustomerOrderId() : orderIdGeneratorService.generateCustomerOrderId());
        orderDetails.setVehicleModelId(vehicleModel);
        orderDetails.setVehicleVariantId(vehicleVariant);
        orderDetails.setCustomerName(response.getCustomerName());
        orderDetails.setPhoneNumber(response.getPhoneNumber());
        orderDetails.setEmail(response.getEmail());
        orderDetails.setPermanentAddress(response.getPermanentAddress());
        orderDetails.setCurrentAddress(response.getCurrentAddress());
        orderDetails.setAadharNo(response.getAadharNo());
        orderDetails.setPanNo(response.getPanNo());
        orderDetails.setModelName(response.getModelName());
        orderDetails.setFuelType(response.getFuelType());
        orderDetails.setColour(response.getColour());
        orderDetails.setTransmissionType(response.getTransmissionType());
        orderDetails.setVariant(response.getVariant());
        orderDetails.setQuantity(response.getQuantity());
        orderDetails.setPaymentMode(response.getPaymentMode());
        orderDetails.setOrderStatus(response.getOrderStatus());
        return orderDetails;
    }

    private OrderResponse mapToOrderResponseFromDetails(VehicleOrderDetails orderDetails) {
        OrderResponse response = new OrderResponse();
        response.setCustomerOrderId(orderDetails.getCustomerOrderId());
        response.setVehicleModelId(orderDetails.getVehicleModelId().getVehicleModelId());
        response.setVehicleVariantId(orderDetails.getVehicleVariantId().getVehicleVariantId());
        response.setCustomerName(orderDetails.getCustomerName());
        response.setPhoneNumber(orderDetails.getPhoneNumber());
        response.setEmail(orderDetails.getEmail());
        response.setPermanentAddress(orderDetails.getPermanentAddress());
        response.setCurrentAddress(orderDetails.getCurrentAddress());
        response.setAadharNo(orderDetails.getAadharNo());
        response.setPanNo(orderDetails.getPanNo());
        response.setModelName(orderDetails.getModelName());
        response.setFuelType(orderDetails.getFuelType());
        response.setColour(orderDetails.getColour());
        response.setTransmissionType(orderDetails.getTransmissionType());
        response.setVariant(orderDetails.getVariant());
        response.setQuantity(orderDetails.getQuantity());
        response.setPaymentMode(orderDetails.getPaymentMode());
        response.setOrderStatus(orderDetails.getOrderStatus());
        return response;
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }

    public long getPendingOrders() {
        return orderRepository.countByOrderStatus(OrderStatus.PENDING);
    }

    public long getFinancePendingOrders() {
        return orderRepository.countByOrderStatus(OrderStatus.FINANCE_PENDING);
    }

    public long getClosedOrders() {
        return orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
    }

    public List<VehicleOrderGridDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> new VehicleOrderGridDTO(
                        order.getCustomerOrderId(),
                        order.getCustomerName(),
                        order.getModelName(),
                        order.getQuantity(),
                        order.getVariant(),
                        order.getOrderStatus()
                ))
                .collect(Collectors.toList());
    }
}