package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleOrderGridDTO;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.DeliveryStatus;
import com.vehicle.salesmanagement.enums.FinanceStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
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
    private final FinanceDetailsRepository financeDetailsRepository;
    private final DeliveryDetailsRepository deliveryDetailsRepository;

    @Transactional
    public OrderResponse checkAndBlockStock(OrderRequest orderRequest) {
        if (orderRequest.getModelName() == null || orderRequest.getVariant() == null || orderRequest.getColour() == null ||
                orderRequest.getTransmissionType() == null || orderRequest.getFuelType() == null) {
            log.warn("Invalid request for customerOrderId: {}. Missing required fields.", orderRequest.getCustomerOrderId());
            return placeManufacturerOrder(orderRequest);
        }

        VehicleVariant variant = variantRepository.findById(orderRequest.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found: " + orderRequest.getVehicleVariantId()));
        List<StockDetails> stocks = stockRepository.findByModelNameAndVehicleVariantIdAndStockStatus(
                orderRequest.getModelName(), variant, StockStatus.AVAILABLE);

        if (stocks.isEmpty()) {
            log.info("No available stock for modelName: {}, vehicleVariantId: {}. Placing manufacturer order.",
                    orderRequest.getModelName(), orderRequest.getVehicleVariantId());
            return placeManufacturerOrder(orderRequest);
        }

        // Sort stocks by stockArrivalDate (ascending) to prioritize older stock
        StockDetails stock = stocks.stream()
                .filter(s -> orderRequest.getVariant().equalsIgnoreCase(s.getVariant()))
                .filter(s -> orderRequest.getColour().equals(s.getColour()))
                .filter(s -> orderRequest.getTransmissionType().equals(s.getTransmissionType()))
                .filter(s -> orderRequest.getFuelType().equals(s.getFuelType()))
                .filter(s -> s.getQuantity() >= orderRequest.getQuantity())
                .sorted(Comparator.comparing(
                        StockDetails::getStockArrivalDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )) // Oldest first
                .findFirst()
                .orElse(null);

        if (stock == null) {
            log.info("No matching stock found for modelName: {}, vehicleVariantId: {}, variant: {}. Placing manufacturer order.",
                    orderRequest.getModelName(), orderRequest.getVehicleVariantId(), orderRequest.getVariant());
            return placeManufacturerOrder(orderRequest);
        }

        stock.setQuantity(stock.getQuantity() - orderRequest.getQuantity());
        if (stock.getQuantity() == 0) {
            stock.setStockStatus(StockStatus.DEPLETED);
        }
        stock = stockRepository.save(stock);
        if (stock.getStockId() == null) {
            throw new IllegalStateException("Stock ID is null after saving StockDetails");
        }
        log.info("Stock ID: {} (arrival date: {}) blocked for modelName: {}, variant: {}",
                stock.getStockId(), stock.getStockArrivalDate(), stock.getModelName(), stock.getVariant());

        historyService.saveStockHistory(stock, "Stock Blocked for Order: " + orderRequest.getCustomerOrderId());

        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.BLOCKED);
        return response;
    }

//    @Transactional
//    public OrderResponse checkAndReserveMddpStock(OrderRequest orderRequest) {
//        VehicleVariant variant = variantRepository.findById(orderRequest.getVehicleVariantId())
//                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + orderRequest.getVehicleVariantId()));
//
//        Optional<MddpStock> mddpStockOptional = mddpStockRepository.findByVehicleVariantIdAndStockStatus(variant, StockStatus.AVAILABLE);
//        log.info("Found MDDP stock for variantId: {}, exists: {}", orderRequest.getVehicleVariantId(), mddpStockOptional.isPresent());
//
//        if (mddpStockOptional.isPresent()) {
//            MddpStock mddpStock = mddpStockOptional.get();
//
//            boolean exactMatch = mddpStock.getQuantity() >= orderRequest.getQuantity()
//                    && mddpStock.getColour().equalsIgnoreCase(orderRequest.getColour())
//                    && mddpStock.getFuelType().equalsIgnoreCase(orderRequest.getFuelType())
//                    && mddpStock.getTransmissionType().equalsIgnoreCase(orderRequest.getTransmissionType())
//                    && mddpStock.getVariant().equalsIgnoreCase(orderRequest.getVariant());
//
//            if (exactMatch) {
//                VehicleModel vehicleModel = vehicleModelRepository.findById(orderRequest.getVehicleModelId())
//                        .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + orderRequest.getVehicleModelId()));
//
//                StockDetails newStock = new StockDetails();
//                newStock.setVehicleVariantId(variant);
//                newStock.setVehicleModelId(vehicleModel);
//                newStock.setModelName(mddpStock.getModelName() != null ? mddpStock.getModelName() : null);
//                newStock.setSuffix(mddpStock.getSuffix() != null ? mddpStock.getSuffix() : null);
//                newStock.setFuelType(orderRequest.getFuelType());
//                newStock.setColour(orderRequest.getColour());
//                newStock.setEngineColour(mddpStock.getEngineColour() != null ? mddpStock.getEngineColour() : null);
//                newStock.setTransmissionType(orderRequest.getTransmissionType());
//                newStock.setVariant(orderRequest.getVariant());
//                newStock.setInteriorColour(mddpStock.getInteriorColour() != null ? mddpStock.getInteriorColour() : null);
//                newStock.setQuantity(orderRequest.getQuantity());
//                newStock.setStockStatus(StockStatus.AVAILABLE);
//                newStock.setStockArrivalDate(String.valueOf(LocalDate.now()));
//                stockRepository.save(newStock);
//
//                mddpStock.setQuantity(mddpStock.getQuantity() - orderRequest.getQuantity());
//                if (mddpStock.getQuantity() == 0) {
//                    mddpStock.setStockStatus(StockStatus.DEPLETED);
//                }
//                mddpStockRepository.save(mddpStock);
//
//                historyService.saveStockHistory(newStock, "Stock Transferred from MDDP for Order: " + orderRequest.getCustomerOrderId());
//
//                log.info("Stock transferred from MDDP to stock_details for order ID: {}", orderRequest.getCustomerOrderId());
//
//                return checkAndBlockStock(orderRequest);
//            } else {
//                log.info("MDDP stock exists but does not match requested attributes for order ID: {}", orderRequest.getCustomerOrderId());
//            }
//        }
//
//        log.info("No MDDP stock available or matched for order ID: {}, placing manufacturer order", orderRequest.getCustomerOrderId());
//        return placeManufacturerOrder(orderRequest);
//    }
@Transactional
public OrderResponse checkAndReserveMddpStock(OrderRequest orderRequest) {
    VehicleVariant variant = variantRepository.findById(orderRequest.getVehicleVariantId())
            .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + orderRequest.getVehicleVariantId()));

    Optional<MddpStock> mddpStockOptional = mddpStockRepository.findByVehicleVariantIdAndStockStatus(variant, StockStatus.AVAILABLE);
    log.info("Found MDDP stock for variantId: {}, exists: {}", orderRequest.getVehicleVariantId(), mddpStockOptional.isPresent());

    if (mddpStockOptional.isPresent()) {
        MddpStock mddpStock = mddpStockOptional.get();

        // ✅ Match all key fields from MDDP stock against the request
        boolean exactMatch = mddpStock.getQuantity() >= orderRequest.getQuantity()
                && mddpStock.getColour().equalsIgnoreCase(orderRequest.getColour())
                && mddpStock.getFuelType().equalsIgnoreCase(orderRequest.getFuelType())
                && mddpStock.getTransmissionType().equalsIgnoreCase(orderRequest.getTransmissionType())
                && mddpStock.getVariant().equalsIgnoreCase(orderRequest.getVariant());

        if (exactMatch) {
            VehicleModel vehicleModel = vehicleModelRepository.findById(orderRequest.getVehicleModelId())
                    .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + orderRequest.getVehicleModelId()));

            // ✅ Create new StockDetails with proper null handling
            StockDetails newStock = new StockDetails();
            newStock.setVehicleVariantId(variant);
            newStock.setVehicleModelId(vehicleModel);
            newStock.setModelName(mddpStock.getModelName() != null ? mddpStock.getModelName() : null);
            newStock.setSuffix(mddpStock.getSuffix() != null ? mddpStock.getSuffix() : null);
            newStock.setFuelType(orderRequest.getFuelType());
            newStock.setColour(orderRequest.getColour());
            newStock.setEngineColour(mddpStock.getEngineColour() != null ? mddpStock.getEngineColour() : null);
            newStock.setTransmissionType(orderRequest.getTransmissionType());
            newStock.setVariant(orderRequest.getVariant());
            newStock.setInteriorColour(mddpStock.getInteriorColour() != null ? mddpStock.getInteriorColour() : null);
            newStock.setQuantity(orderRequest.getQuantity());
            newStock.setStockArrivalDate(LocalDate.now().toString());
            newStock.setStockStatus(StockStatus.AVAILABLE);
            stockRepository.save(newStock);

            // Deduct quantity from MDDP stock
            mddpStock.setQuantity(mddpStock.getQuantity() - orderRequest.getQuantity());
            if (mddpStock.getQuantity() == 0) {
                mddpStock.setStockStatus(StockStatus.DEPLETED);
            }
            mddpStockRepository.save(mddpStock);

            // Save stock history
            historyService.saveStockHistory(newStock, "Stock Transferred from MDDP for Order: " + orderRequest.getCustomerOrderId());
            log.info("Stock transferred from MDDP to stock_details for order ID: {}", orderRequest.getCustomerOrderId());

            // Try blocking again now that new stock was added
            return checkAndBlockStock(orderRequest);
        } else {
            log.info("MDDP stock exists but does not match requested attributes for order ID: {}", orderRequest.getCustomerOrderId());
        }
    }

    // If no MDDP stock matched → fallback to manufacturer
    log.info("No MDDP stock available or matched for order ID: {}, placing manufacturer order", orderRequest.getCustomerOrderId());
    return placeManufacturerOrder(orderRequest);
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
        // Set expectedDeliveryDate if provided, otherwise keep it null
        orderDetails.setExpectedDeliveryDate(orderRequest.getExpectedDeliveryDate());
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
        List<StockDetails> stocks = stockRepository.findByModelNameAndVehicleVariantIdAndStockStatus(
                orderDetails.getModelName(), variant, StockStatus.AVAILABLE);

        StockDetails stock = stocks.stream()
                .filter(s -> orderDetails.getVariant() != null && orderDetails.getVariant().equalsIgnoreCase(s.getVariant()))
                .filter(s -> orderDetails.getColour() != null && orderDetails.getColour().equals(s.getColour()))
                .filter(s -> orderDetails.getTransmissionType() != null && orderDetails.getTransmissionType().equals(s.getTransmissionType()))
                .filter(s -> orderDetails.getFuelType() != null && orderDetails.getFuelType().equals(s.getFuelType()))
                .sorted(Comparator.comparing(
                        StockDetails::getStockArrivalDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )) // Prioritize older stock for restoration
                .findFirst()
                .orElse(null);

        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + orderDetails.getQuantity());
            stock.setStockStatus(StockStatus.AVAILABLE);
            stockRepository.save(stock);
            historyService.saveStockHistory(stock, "Stock Restored for Canceled Order: " + customerOrderId);
            log.info("Restored stock ID: {} (arrival date: {}) for canceled order: {}", stock.getStockId(), stock.getStockArrivalDate(), customerOrderId);
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
            newStock.setModelName(orderDetails.getModelName());
            newStock.setStockArrivalDate(String.valueOf(LocalDate.now()));
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
        response.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
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
        orderDetails.setExpectedDeliveryDate(response.getExpectedDeliveryDate());
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
        response.setExpectedDeliveryDate(orderDetails.getExpectedDeliveryDate());
        return response;
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }

    public long getPendingOrders() {
        return orderRepository.countByOrderStatus(OrderStatus.PENDING);
    }

    public long getFinancePendingOrders() {
        return financeDetailsRepository.countByFinanceStatus(FinanceStatus.PENDING);
    }

    public long getClosedOrders() {
        return deliveryDetailsRepository.countByDeliveryStatus(DeliveryStatus.DELIVERED);
    }

    public List<VehicleOrderGridDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> new VehicleOrderGridDTO(
                        order.getCustomerOrderId(),
                        order.getCustomerName(),
                        order.getModelName(),
                        order.getQuantity(),
                        order.getVariant(),
                        order.getOrderStatus(),
                        order.getExpectedDeliveryDate()
                ))
                .collect(Collectors.toList());
    }
}