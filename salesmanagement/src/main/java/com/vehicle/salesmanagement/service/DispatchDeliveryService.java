package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.domain.entity.model.DeliveryDetails;
import com.vehicle.salesmanagement.domain.entity.model.DispatchDetails;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.DeliveryStatus;
import com.vehicle.salesmanagement.enums.DispatchStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.DeliveryDetailsRepository;
import com.vehicle.salesmanagement.repository.DispatchDetailsRepository;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchDeliveryService {

    private final DispatchDetailsRepository dispatchDetailsRepository;
    private final DeliveryDetailsRepository deliveryDetailsRepository;
    private final VehicleOrderDetailsRepository vehicleOrderDetailsRepository;
    private final HistoryService historyService;

    @Transactional
    public DispatchResponse initiateDispatch(DispatchRequest request) {
        log.info("Initiating dispatch for order ID: {}", request.getCustomerOrderId());

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(request.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getCustomerOrderId()));

        // Check if order is already in DISPATCHED or later state
        if (orderDetails.getOrderStatus().equals(OrderStatus.DISPATCHED) ||
                orderDetails.getOrderStatus().equals(OrderStatus.DELIVERED) ||
                orderDetails.getOrderStatus().equals(OrderStatus.COMPLETED)) {
            log.warn("Order ID {} is already in status {}. Skipping dispatch.",
                    request.getCustomerOrderId(), orderDetails.getOrderStatus());
            return mapToDispatchResponse(
                    dispatchDetailsRepository.findByCustomerOrderId(request.getCustomerOrderId()),
                    orderDetails
            );
        }

        // Validate that order is in ALLOTTED state
        if (!orderDetails.getOrderStatus().equals(OrderStatus.ALLOTTED)) {
            throw new IllegalStateException("Order must be ALLOTTED to initiate dispatch. Current status: " + orderDetails.getOrderStatus());
        }

        DispatchDetails existing = dispatchDetailsRepository.findByCustomerOrderId(request.getCustomerOrderId());
        if (existing != null) {
            log.warn("Dispatch details already exist for order ID: {}. Skipping creation.",
                    request.getCustomerOrderId());
            return mapToDispatchResponse(existing, orderDetails);
        }

        DispatchDetails dispatchDetails = new DispatchDetails();
        dispatchDetails.setCustomerOrderId(request.getCustomerOrderId());
        dispatchDetails.setCustomerName(orderDetails.getCustomerName());
        dispatchDetails.setDispatchStatus(DispatchStatus.PREPARING);
        dispatchDetails.setDispatchedBy(request.getDispatchedBy());
        dispatchDetails.setDispatchDate(LocalDateTime.now());
        //dispatchDetails.setCreatedAt(LocalDateTime.now());
        //dispatchDetails.setUpdatedAt(LocalDateTime.now());
        dispatchDetailsRepository.save(dispatchDetails);

        historyService.saveOrderHistory(orderDetails, request.getDispatchedBy(), OrderStatus.DISPATCHED);
        orderDetails.setOrderStatus(OrderStatus.DISPATCHED);
        //orderDetails.setUpdatedAt(LocalDateTime.now());
        vehicleOrderDetailsRepository.save(orderDetails);
        log.info("Vehicle order details updated to DISPATCHED for order ID: {}", request.getCustomerOrderId());

        historyService.saveDispatchHistory(dispatchDetails, request.getDispatchedBy());
        return mapToDispatchResponse(dispatchDetails, orderDetails);
    }

    @Transactional
    public DeliveryResponse confirmDelivery(DeliveryRequest request) {
        log.info("Confirming delivery for order ID: {}", request.getCustomerOrderId());

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(request.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getCustomerOrderId()));

        // Check if order is already in DELIVERED or later state
        if (orderDetails.getOrderStatus().equals(OrderStatus.DELIVERED) ||
                orderDetails.getOrderStatus().equals(OrderStatus.COMPLETED)) {
            log.warn("Order ID {} is already in status {}. Skipping delivery confirmation.",
                    request.getCustomerOrderId(), orderDetails.getOrderStatus());
            DeliveryDetails existing = deliveryDetailsRepository.findByCustomerOrderId(request.getCustomerOrderId());
            return mapToDeliveryResponse(existing != null ? existing : new DeliveryDetails(), orderDetails);
        }

        // Validate that order is in DISPATCHED state
        if (!orderDetails.getOrderStatus().equals(OrderStatus.DISPATCHED)) {
            throw new IllegalStateException("Order must be DISPATCHED to confirm delivery. Current status: " + orderDetails.getOrderStatus());
        }

        DeliveryDetails existing = deliveryDetailsRepository.findByCustomerOrderId(request.getCustomerOrderId());
        if (existing != null) {
            log.warn("Delivery details already exist for order ID: {}. Skipping creation.",
                    request.getCustomerOrderId());
            return mapToDeliveryResponse(existing, orderDetails);
        }

        DeliveryDetails deliveryDetails = new DeliveryDetails();
        deliveryDetails.setCustomerOrderId(request.getCustomerOrderId());
        deliveryDetails.setCustomerName(orderDetails.getCustomerName());
        deliveryDetails.setDeliveryStatus(DeliveryStatus.DELIVERED);
        deliveryDetails.setDeliveryDate(LocalDateTime.now());
        deliveryDetails.setDeliveredBy(request.getDeliveredBy());
        deliveryDetails.setRecipientName(request.getRecipientName());
        //deliveryDetails.setCreatedAt(LocalDateTime.now());
        //deliveryDetails.setUpdatedAt(LocalDateTime.now());
        deliveryDetailsRepository.save(deliveryDetails);

        historyService.saveOrderHistory(orderDetails, request.getDeliveredBy(), OrderStatus.DELIVERED);
        orderDetails.setOrderStatus(OrderStatus.DELIVERED);
        //orderDetails.setUpdatedAt(LocalDateTime.now());
        vehicleOrderDetailsRepository.save(orderDetails);
        log.info("Vehicle order details updated to DELIVERED for order ID: {}", request.getCustomerOrderId());

        historyService.saveDeliveryHistory(deliveryDetails, request.getDeliveredBy());
        return mapToDeliveryResponse(deliveryDetails, orderDetails);
    }

    private DispatchResponse mapToDispatchResponse(DispatchDetails dispatchDetails, VehicleOrderDetails orderDetails) {
        if (dispatchDetails == null) {
            log.warn("No dispatch details found for order ID: {}. Creating response with order details only.",
                    orderDetails.getCustomerOrderId());
            DispatchResponse response = new DispatchResponse();
            response.setCustomerOrderId(orderDetails.getCustomerOrderId());
            response.setCustomerName(orderDetails.getCustomerName());
            response.setOrderStatus(orderDetails.getOrderStatus());
            response.setModelName(orderDetails.getModelName());
            response.setVariant(orderDetails.getVariant());
            return response;
        }

        DispatchResponse response = new DispatchResponse();
        response.setDispatchId(dispatchDetails.getDispatchId());
        response.setCustomerOrderId(dispatchDetails.getCustomerOrderId());
        response.setCustomerName(dispatchDetails.getCustomerName());
        response.setDispatchStatus(dispatchDetails.getDispatchStatus());
        response.setOrderStatus(orderDetails.getOrderStatus());
        response.setModelName(orderDetails.getModelName());
        response.setVariant(orderDetails.getVariant());
        response.setDispatchDate(dispatchDetails.getDispatchDate());
        response.setDispatchedBy(dispatchDetails.getDispatchedBy());
        //response.setCreatedAt(dispatchDetails.getCreatedAt());
        //response.setUpdatedAt(dispatchDetails.getUpdatedAt());
        return response;
    }

    private DeliveryResponse mapToDeliveryResponse(DeliveryDetails deliveryDetails, VehicleOrderDetails orderDetails) {
        DeliveryResponse response = new DeliveryResponse();
        response.setCustomerOrderId(orderDetails.getCustomerOrderId());
        response.setCustomerName(orderDetails.getCustomerName());
        response.setOrderStatus(orderDetails.getOrderStatus());
        response.setModelName(orderDetails.getModelName());
        response.setVariant(orderDetails.getVariant());

        if (deliveryDetails.getDeliveryId() == null) {
            log.warn("No delivery details found for order ID: {}. Returning response with order details only.",
                    orderDetails.getCustomerOrderId());
            return response;
        }

        response.setDeliveryId(deliveryDetails.getDeliveryId());
        response.setDeliveryStatus(deliveryDetails.getDeliveryStatus());
        response.setDeliveryDate(deliveryDetails.getDeliveryDate());
        response.setDeliveredBy(deliveryDetails.getDeliveredBy());
        response.setRecipientName(deliveryDetails.getRecipientName());
        //response.setCreatedAt(deliveryDetails.getCreatedAt());
        //response.setUpdatedAt(deliveryDetails.getUpdatedAt());
        return response;
    }
}