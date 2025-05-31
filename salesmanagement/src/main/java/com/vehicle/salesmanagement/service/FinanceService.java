package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.domain.entity.model.FinanceDetails;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.FinanceStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.FinanceDetailsRepository;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinanceDetailsRepository financeDetailsRepository;
    private final VehicleOrderDetailsRepository vehicleOrderDetailsRepository;
    private final HistoryService historyService;

    @Transactional
    public FinanceResponse createFinanceDetails(FinanceRequest request) {
        log.info("Creating finance details for order ID: {}", request.getCustomerOrderId());

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findById(request.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getCustomerOrderId()));
        if (!orderDetails.getOrderStatus().equals(OrderStatus.BLOCKED)) {
            throw new IllegalStateException("Order must be in BLOCKED status to initiate finance: " + request.getCustomerOrderId());
        }

        FinanceDetails existing = financeDetailsRepository.findByCustomerOrderId(request.getCustomerOrderId());
        if (existing != null) {
            log.warn("Finance details already exist for order ID: {} with finance status: {}", request.getCustomerOrderId(), existing.getFinanceStatus());
            throw new IllegalStateException("Finance details already exist for order ID: " + request.getCustomerOrderId());
        }

        FinanceDetails financeDetails = new FinanceDetails();
        financeDetails.setCustomerOrderId(request.getCustomerOrderId());
        financeDetails.setCustomerName(request.getCustomerName());
        financeDetails.setFinanceStatus(FinanceStatus.PENDING);
        financeDetails.setCreatedAt(LocalDateTime.now());
        financeDetails.setUpdatedAt(LocalDateTime.now());
        financeDetailsRepository.save(financeDetails);

        return mapToFinanceResponse(financeDetails, orderDetails);
    }

    @Transactional(readOnly = true)
    public FinanceResponse getFinanceDetails(Long customerOrderId) {
        log.info("Retrieving finance details for order ID: {}", customerOrderId);

        FinanceDetails financeDetails = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (financeDetails == null) {
            throw new RuntimeException("Finance details not found for order ID: " + customerOrderId);
        }

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + customerOrderId));

        return mapToFinanceResponse(financeDetails, orderDetails);
    }

    @Transactional
    public FinanceResponse approveFinance(Long customerOrderId, String approvedBy) {
        log.info("Approving finance for order ID: {}", customerOrderId);

        FinanceDetails financeDetails = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (financeDetails == null) {
            throw new RuntimeException("Finance details not found for order ID: " + customerOrderId);
        }
        if (financeDetails.getFinanceStatus() != FinanceStatus.PENDING) {
            throw new IllegalStateException("Finance request is not in PENDING status for order ID: " + customerOrderId);
        }

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + customerOrderId));

        historyService.saveFinanceHistory(financeDetails, approvedBy);
        financeDetails.setFinanceStatus(FinanceStatus.APPROVED);
        financeDetails.setApprovedBy(approvedBy);
        financeDetails.setUpdatedAt(LocalDateTime.now());
        financeDetailsRepository.save(financeDetails);

        historyService.saveOrderHistory(orderDetails, approvedBy, OrderStatus.ALLOTTED); // Pass new status
        orderDetails.setOrderStatus(OrderStatus.ALLOTTED);
        //orderDetails.setUpdatedAt(LocalDateTime.now());
        vehicleOrderDetailsRepository.save(orderDetails);

        return mapToFinanceResponse(financeDetails, orderDetails);
    }

    @Transactional
    public FinanceResponse rejectFinance(Long customerOrderId, String rejectedBy) {
        log.info("Rejecting finance for order ID: {}", customerOrderId);

        FinanceDetails financeDetails = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (financeDetails == null) {
            throw new RuntimeException("Finance details not found for order ID: " + customerOrderId);
        }
        if (financeDetails.getFinanceStatus() != FinanceStatus.PENDING) {
            throw new IllegalStateException("Finance request is not in PENDING status for order ID: " + customerOrderId);
        }

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findById(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + customerOrderId));

        historyService.saveFinanceHistory(financeDetails, rejectedBy);
        financeDetails.setFinanceStatus(FinanceStatus.REJECTED);
        financeDetails.setRejectedBy(rejectedBy);
        financeDetails.setUpdatedAt(LocalDateTime.now());
        financeDetailsRepository.save(financeDetails);

        historyService.saveOrderHistory(orderDetails, rejectedBy, OrderStatus.PENDING); // Pass new status
        orderDetails.setOrderStatus(OrderStatus.PENDING);
        //orderDetails.setUpdatedAt(LocalDateTime.now());
        vehicleOrderDetailsRepository.save(orderDetails);

        return mapToFinanceResponse(financeDetails, orderDetails);
    }

    private FinanceResponse mapToFinanceResponse(FinanceDetails financeDetails, VehicleOrderDetails orderDetails) {
        FinanceResponse response = new FinanceResponse();
        response.setFinanceId(financeDetails.getFinanceId());
        response.setCustomerOrderId(financeDetails.getCustomerOrderId());
        response.setCustomerName(financeDetails.getCustomerName());
        response.setFinanceStatus(financeDetails.getFinanceStatus());
        response.setOrderStatus(orderDetails.getOrderStatus());
        response.setModelName(orderDetails.getModelName());
        response.setVariant(orderDetails.getVariant());
        response.setApprovedBy(financeDetails.getApprovedBy());
        response.setRejectedBy(financeDetails.getRejectedBy());
        response.setCreatedAt(financeDetails.getCreatedAt());
        response.setUpdatedAt(financeDetails.getUpdatedAt());
        return response;
    }
}