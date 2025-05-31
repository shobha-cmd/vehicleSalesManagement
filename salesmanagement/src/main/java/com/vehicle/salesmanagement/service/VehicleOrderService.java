package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.VehicleModelRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.VehicleVariantRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleModelResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleOrderGridDTO;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @Transactional
    public OrderResponse checkAndBlockStock(OrderRequest orderRequest) {
        VehicleVariant variant = variantRepository.findById(orderRequest.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Variant not found: " + orderRequest.getVehicleVariantId()));
        List<StockDetails> stocks = stockRepository.findByVehicleVariantAndStockStatus(variant, StockStatus.AVAILABLE);

        if (stocks.isEmpty()) {
            return placeManufacturerOrder(orderRequest);
        }

        StockDetails stock = stocks.stream()
                .filter(s -> s.getQuantity() >= orderRequest.getQuantity()
                        && s.getColour().equals(orderRequest.getColour())
                        && s.getFuelType().equals(orderRequest.getFuelType())
                        && s.getTransmissionType().equals(orderRequest.getTransmissionType()))
                .findFirst()
                .orElse(null);

        if (stock == null) {
            return placeManufacturerOrder(orderRequest);
        }

        stock.setQuantity(stock.getQuantity() - orderRequest.getQuantity());
        if (stock.getQuantity() == 0) {
            stock.setStockStatus(StockStatus.DEPLETED);
        }
        // Save and ensure stockId is populated
        stock = stockRepository.save(stock);
        if (stock.getStockId() == null) {
            log.error("Stock ID is null after saving StockDetails for VIN: {}", stock.getVinNumber());
            throw new IllegalStateException("Stock ID is null after saving StockDetails");
        }
        log.info("Stock ID: {} for VIN: {}", stock.getStockId(), stock.getVinNumber());

        // Save history with the updated stock
        //historyService.saveStockHistory(stock, orderRequest.getUpdatedBy() != null ? orderRequest.getUpdatedBy() : "system",
          //      "Stock Blocked for Order: " + orderRequest.getCustomerOrderId());

        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.BLOCKED);
       // response.setCreatedAt(LocalDateTime.now());
        return response;
    }
    @Transactional
    public OrderResponse checkAndReserveMddpStock(OrderRequest orderRequest) {
        VehicleVariant vehicleVariant = variantRepository.findById(orderRequest.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + orderRequest.getVehicleVariantId()));
        Optional<MddpStock> mddpStockOptional = mddpStockRepository.findByVehicleVariantIdAndStockStatus(
                vehicleVariant, StockStatus.AVAILABLE);
        if (mddpStockOptional.isPresent()) {
            MddpStock mddpStock = mddpStockOptional.get();
            if (mddpStock.getQuantity() >= orderRequest.getQuantity()) {
                VehicleModel vehicleModel = vehicleModelRepository.findById(orderRequest.getVehicleModelId())
                        .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + orderRequest.getVehicleModelId()));

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
                stockRepository.save(newStock);

                mddpStock.setQuantity(mddpStock.getQuantity() - orderRequest.getQuantity());
                if (mddpStock.getQuantity() == 0) {
                    mddpStock.setStockStatus(StockStatus.DEPLETED);
                }
                mddpStockRepository.save(mddpStock);
               // historyService.saveStockHistory(newStock, orderRequest.getUpdatedBy() != null ? orderRequest.getUpdatedBy() : "system", "Stock Transferred from MDDP for Order: " + orderRequest.getCustomerOrderId());

                OrderResponse response = mapToOrderResponse(orderRequest);
                response.setOrderStatus(OrderStatus.BLOCKED);
                //response.setCreatedAt(LocalDateTime.now());
                return response;
            }
        }
        return placeManufacturerOrder(orderRequest);
    }

    @Transactional
    public OrderResponse placeManufacturerOrder(OrderRequest orderRequest) {
        OrderResponse response = mapToOrderResponse(orderRequest);
        response.setOrderStatus(OrderStatus.PENDING);
        return response;
    }

    @Transactional
    public OrderResponse confirmOrder(OrderResponse orderResponse) {
        log.info("Confirming order for customer: {}, customerOrderId: {}", orderResponse.getCustomerName(), orderResponse.getCustomerOrderId());

        if (orderResponse.getCustomerOrderId() == null) {
            log.error("CustomerOrderId is null in confirmOrder for customer: {}", orderResponse.getCustomerName());
            throw new IllegalArgumentException("CustomerOrderId is required to confirm order");
        }

        VehicleOrderDetails orderDetails = orderRepository.findById(orderResponse.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found with customerOrderId: " + orderResponse.getCustomerOrderId()));

        if (orderDetails.getOrderStatus() != OrderStatus.BLOCKED) {
            log.error("Order with customerOrderId: {} must be in BLOCKED status to confirm, current status: {}", orderResponse.getCustomerOrderId(), orderDetails.getOrderStatus());
            throw new IllegalStateException("Order must be in BLOCKED status to confirm, current status: " + orderDetails.getOrderStatus());
        }

        orderDetails.setOrderStatus(OrderStatus.CONFIRMED);
       // orderDetails.setUpdatedAt(LocalDateTime.now());
        orderDetails = orderRepository.save(orderDetails);
        log.info("Saved VehicleOrderDetails with customerOrderId: {}", orderDetails.getCustomerOrderId());

        //historyService.saveOrderHistory(orderDetails, orderDetails.getUpdatedBy(), OrderStatus.CONFIRMED);

        return mapToOrderResponseFromDetails(orderDetails);
    }

    public OrderResponse notifyCustomerWithTentativeDelivery(OrderRequest orderRequest) {
        OrderResponse orderResponse = mapToOrderResponse(orderRequest);
        VehicleOrderDetails orderDetails = mapToOrderDetails(orderResponse);
        orderDetails.setOrderStatus(OrderStatus.NOTIFIED);
        //orderDetails.setUpdatedAt(LocalDateTime.now());
        orderDetails = orderRepository.save(orderDetails);
        //historyService.saveOrderHistory(orderDetails, orderDetails.getUpdatedBy(), OrderStatus.NOTIFIED);
        orderResponse.setOrderStatus(OrderStatus.NOTIFIED);
       // orderResponse.setUpdatedAt(LocalDateTime.now());
        return orderResponse;
    }

    @Transactional
    public OrderResponse cancelOrder(Long customerOrderId) {
        VehicleOrderDetails orderDetails = orderRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found with customerOrderId: " + customerOrderId));

        if (orderDetails.getOrderStatus() == OrderStatus.COMPLETED || orderDetails.getOrderStatus() == OrderStatus.CANCELED) {
            log.error("Order with customerOrderId: {} cannot be canceled. Current status: {}", customerOrderId, orderDetails.getOrderStatus());
            throw new IllegalStateException("Order with customerOrderId: " + customerOrderId + " cannot be canceled. Current status: " + orderDetails.getOrderStatus());
        }

        VehicleVariant variant = orderDetails.getVehicleVariantId();
        VehicleModel model = orderDetails.getVehicleModelId();
        List<StockDetails> stocks = stockRepository.findByVehicleVariantAndVehicleModel(variant, model);

        StockDetails stock = stocks.stream()
                .filter(s -> s.getColour().equals(orderDetails.getColour())
                        && s.getFuelType().equals(orderDetails.getFuelType())
                        && s.getTransmissionType().equals(orderDetails.getTransmissionType()))
                .findFirst()
                .orElse(null);

        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + orderDetails.getQuantity());
            stock.setStockStatus(StockStatus.AVAILABLE);
            stockRepository.save(stock);
            historyService.saveStockHistory(stock, "system", "Stock Restored for Canceled Order: " + customerOrderId);
        } else {
            StockDetails newStock = new StockDetails();
            newStock.setVehicleVariant(variant);
            newStock.setVehicleModel(model);
            newStock.setColour(orderDetails.getColour());
            newStock.setFuelType(orderDetails.getFuelType());
            newStock.setTransmissionType(orderDetails.getTransmissionType());
            newStock.setVariant(orderDetails.getVariant());
            newStock.setQuantity(orderDetails.getQuantity());
            newStock.setStockStatus(StockStatus.AVAILABLE);
            newStock.setCreatedAt(LocalDateTime.now());
            stockRepository.save(newStock);
        }

        historyService.saveOrderHistory(orderDetails, "system", OrderStatus.CANCELED);
        orderDetails.setOrderStatus(OrderStatus.CANCELED);
        //orderDetails.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(orderDetails);

        return mapToOrderResponseFromDetails(orderDetails);
    }

    public OrderResponse mapToOrderResponse(OrderRequest request) {
        if (request.getCustomerOrderId() == null) {
            log.error("CustomerOrderId is null in OrderRequest for customer: {}", request.getCustomerName());
            return new OrderResponse(null, OrderStatus.PENDING); // This line is now compatible with the new constructor
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
        //BigDecimal totalPrice = request.getTotalPrice();
//        if (totalPrice == null) {
//            BigDecimal quantity = new BigDecimal(request.getQuantity());
//            totalPrice = variant.getPrice().multiply(quantity);
//        }
//        response.setTotalPrice(totalPrice);
//
//        BigDecimal bookingAmount = request.getBookingAmount();
//        if (bookingAmount == null) {
//            bookingAmount = totalPrice.multiply(new BigDecimal("0.1"));
//        }
//        response.setBookingAmount(bookingAmount);

        response.setPaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "");
       // response.setCreatedAt(LocalDateTime.now());
//        response.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "system");
//        response.setUpdatedBy(request.getUpdatedBy() != null ? request.getUpdatedBy() : "system");
        return response;
    }

    private VehicleOrderDetails mapToOrderDetails(OrderResponse response) {
        VehicleModel vehicleModel = vehicleModelRepository.findById(response.getVehicleModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + response.getVehicleModelId()));
        VehicleVariant vehicleVariant = variantRepository.findById(response.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + response.getVehicleVariantId()));

        VehicleOrderDetails orderDetails = new VehicleOrderDetails();
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
//        orderDetails.setTotalPrice(response.getTotalPrice());
//        orderDetails.setBookingAmount(response.getBookingAmount());
        orderDetails.setPaymentMode(response.getPaymentMode());
        orderDetails.setOrderStatus(response.getOrderStatus());
//        orderDetails.setCreatedAt(response.getCreatedAt());
//        orderDetails.setUpdatedAt(response.getUpdatedAt());
//        orderDetails.setCreatedBy(response.getCreatedBy());
//        orderDetails.setUpdatedBy(response.getUpdatedBy());
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
//        response.setTotalPrice(orderDetails.getTotalPrice());
//        response.setBookingAmount(orderDetails.getBookingAmount());
        response.setPaymentMode(orderDetails.getPaymentMode());
//        response.setCreatedAt(orderDetails.getCreatedAt());
//        response.setUpdatedAt(orderDetails.getUpdatedAt());
//        response.setCreatedBy(orderDetails.getCreatedBy());
//        response.setUpdatedBy(orderDetails.getUpdatedBy());
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