package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final VehicleOrderDetailsHistoryRepository orderHistoryRepository;
    private final FinanceDetailsHistoryRepository financeHistoryRepository;
    private final DispatchDetailsHistoryRepository dispatchHistoryRepository;
    private final DeliveryDetailsHistoryRepository deliveryHistoryRepository;
    private final StockDetailsHistoryRepository stockDetailsHistoryRepository;

    @Transactional
    public void saveOrderHistory(VehicleOrderDetails orderDetails, String updatedBy, OrderStatus newStatus) {
        log.info("Saving history for VehicleOrderDetails with ID: {}",
                orderDetails.getCustomerOrderId() != null ? orderDetails.getCustomerOrderId() : "null");

        if (orderDetails.getCustomerOrderId() == null) {
            throw new IllegalStateException("VehicleOrderDetails must be persisted before saving history");
        }

        VehicleOrderDetailsHistory history = new VehicleOrderDetailsHistory();
        history.setVehicleOrderDetailsId(orderDetails);
        history.setCustomerOrderId(orderDetails.getCustomerOrderId());
        history.setCustomerName(orderDetails.getCustomerName());
        history.setAadharNo(orderDetails.getAadharNo());
        history.setColour(orderDetails.getColour());
        history.setCurrentAddress(orderDetails.getCurrentAddress());
        history.setEmail(orderDetails.getEmail());
        history.setFuelType(orderDetails.getFuelType());
        history.setModelName(orderDetails.getModelName());
        history.setOrderStatus(newStatus.name());
        history.setPanNo(orderDetails.getPanNo());
        history.setPaymentMode(orderDetails.getPaymentMode());
        history.setPermanentAddress(orderDetails.getPermanentAddress());
        history.setPhoneNumber(orderDetails.getPhoneNumber());
        history.setQuantity(orderDetails.getQuantity());
        history.setTransmissionType(history.getTransmissionType());
        history.setVariant(history.getVariant());
        history.setVehicleModelId(history.getVehicleModelId());
        history.setVehicleVariantId(history.getVehicleVariantId());
        history.setChangedAt(LocalDateTime.now());
        history.setOrderStatusHistory(String.format("Order status updated from %s to %s at %s",
                orderDetails.getOrderStatus().name(), newStatus.name(), history.getChangedAt()));

        orderHistoryRepository.save(history);
        log.info("VehicleOrderDetailsHistory saved for order ID: {}", orderDetails.getCustomerOrderId());
    }

    @Transactional
    public void saveFinanceHistory(FinanceDetails financeDetails, String updatedBy) {
        log.info("Saving history for FinanceDetails with ID: {}", financeDetails.getFinanceId());
        FinanceDetailsHistory history = new FinanceDetailsHistory();
        history.setFinanceDetails(financeDetails);
        history.setCustomerOrderId(financeDetails.getCustomerOrderId());
        history.setCustomerName(financeDetails.getCustomerName());
        history.setFinanceStatus(financeDetails.getFinanceStatus());
        history.setApprovedBy(financeDetails.getApprovedBy());
        history.setRejectedBy(financeDetails.getRejectedBy());
        history.setChangedAt(LocalDateTime.now());
        history.setFinanceStatusHistory("Finance status changed to: " + financeDetails.getFinanceStatus().name() + " at " + history.getChangedAt());
        financeHistoryRepository.save(history);
        log.info("FinanceDetailsHistory saved for finance ID: {}", financeDetails.getFinanceId());
    }

    @Transactional
    public void saveDispatchHistory(DispatchDetails dispatchDetails, String updatedBy) {
        log.info("Saving history for DispatchDetails with ID: {}", dispatchDetails.getDispatchId());
        DispatchDetailsHistory history = new DispatchDetailsHistory();
        history.setDispatchDetails(dispatchDetails);
        history.setCustomerOrderId(dispatchDetails.getCustomerOrderId());
        history.setCustomerName(dispatchDetails.getCustomerName());
        history.setDispatchStatus(dispatchDetails.getDispatchStatus());
        history.setDispatchDate(dispatchDetails.getDispatchDate());
        history.setDispatchedBy(dispatchDetails.getDispatchedBy());
        history.setChangedAt(LocalDateTime.now());
        history.setDispatchStatusHistory("Dispatch status changed to: " + dispatchDetails.getDispatchStatus().name() + " at " + history.getChangedAt());
        dispatchHistoryRepository.save(history);
        log.info("DispatchDetailsHistory saved for dispatch ID: {}", dispatchDetails.getDispatchId());
    }

    @Transactional
    public void saveDeliveryHistory(DeliveryDetails deliveryDetails, String updatedBy) {
        log.info("Saving history for DeliveryDetails with ID: {}", deliveryDetails.getDeliveryId());
        DeliveryDetailsHistory history = new DeliveryDetailsHistory();
        history.setDeliveryDetails(deliveryDetails);
        history.setCustomerOrderId(deliveryDetails.getCustomerOrderId());
        history.setCustomerName(deliveryDetails.getCustomerName());
        history.setDeliveryStatus(deliveryDetails.getDeliveryStatus());
        history.setDeliveryDate(deliveryDetails.getDeliveryDate());
        history.setDeliveredBy(deliveryDetails.getDeliveredBy());
        history.setRecipientName(deliveryDetails.getRecipientName());
        deliveryHistoryRepository.save(history);
        log.info("DeliveryDetailsHistory saved for delivery ID: {}", deliveryDetails.getDeliveryId());
    }

    @Transactional
    public void saveStockHistory(StockDetails stockDetails, String action) {
        StockDetailsHistory history = new StockDetailsHistory();
        history.setStockId(stockDetails);
        history.setChangedAt(LocalDateTime.now());
        history.setVehicleModelId(stockDetails.getVehicleModelId());
        history.setVehicleVariantId(stockDetails.getVehicleVariantId());
        history.setSuffix(stockDetails.getSuffix());
        history.setFuelType(stockDetails.getFuelType());
        history.setColour(stockDetails.getColour());
        history.setEngineColour(stockDetails.getEngineColour());
        history.setTransmissionType(stockDetails.getTransmissionType());
        history.setVariant(stockDetails.getVariant());
        history.setQuantity(stockDetails.getQuantity());
        history.setStockStatus(stockDetails.getStockStatus().name());
        history.setInteriorColour(stockDetails.getInteriorColour());
        history.setVinNumber(stockDetails.getVinNumber());
        history.setModelName(stockDetails.getModelName());
        history.setStockHistory(action);

        stockDetailsHistoryRepository.save(history);
    }
}