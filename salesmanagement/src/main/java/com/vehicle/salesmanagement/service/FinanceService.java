package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceDTO;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.domain.entity.model.FinanceDetails;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.FinanceStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.FinanceDetailsRepository;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

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

        String customerOrderId = request.getCustomerOrderId();
        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + customerOrderId));
        if (!orderDetails.getOrderStatus().equals(OrderStatus.BLOCKED)) {
            throw new IllegalStateException("Order must be in BLOCKED status to initiate finance: " + customerOrderId);
        }

        FinanceDetails existing = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (existing != null) {
            log.warn("Finance details already exist for order ID: {} with finance status: {}", customerOrderId, existing.getFinanceStatus());
            throw new IllegalStateException("Finance details already exist for order ID: " + customerOrderId);
        }

        FinanceDetails financeDetails = new FinanceDetails();
        financeDetails.setCustomerOrderId(customerOrderId);
        financeDetails.setCustomerName(request.getCustomerName());
        financeDetails.setFinanceStatus(FinanceStatus.PENDING);
        financeDetailsRepository.save(financeDetails);

        return mapToFinanceResponse(financeDetails, orderDetails);
    }

    @Transactional(readOnly = true)
    public FinanceResponse getFinanceDetails(String customerOrderId) {
        log.info("Retrieving finance details for order ID: {}", customerOrderId);

        FinanceDetails financeDetails = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (financeDetails == null) {
            throw new RuntimeException("Finance details not found for order ID: " + customerOrderId);
        }

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + customerOrderId));

        return mapToFinanceResponse(financeDetails, orderDetails);
    }

    @Transactional
    public FinanceResponse approveFinance(String customerOrderId, String approvedBy) {
        log.info("Approving finance for order ID: {}", customerOrderId);

        FinanceDetails financeDetails = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (financeDetails == null) {
            throw new RuntimeException("Finance details not found for order ID: " + customerOrderId);
        }
        if (financeDetails.getFinanceStatus() != FinanceStatus.PENDING) {
            throw new IllegalStateException("Finance request is not in PENDING status for order ID: " + customerOrderId);
        }

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + customerOrderId));

        historyService.saveFinanceHistory(financeDetails, approvedBy, FinanceStatus.APPROVED);
        financeDetails.setFinanceStatus(FinanceStatus.APPROVED);
        financeDetails.setApprovedBy(approvedBy);
        financeDetailsRepository.save(financeDetails);

        historyService.saveOrderHistory(orderDetails, approvedBy, OrderStatus.ALLOTTED);
        orderDetails.setOrderStatus(OrderStatus.ALLOTTED);
        vehicleOrderDetailsRepository.save(orderDetails);

        return mapToFinanceResponse(financeDetails, orderDetails);
    }

    @Transactional
    public FinanceResponse rejectFinance(String customerOrderId, String rejectedBy) {
        log.info("Rejecting finance for order ID: {}", customerOrderId);

        FinanceDetails financeDetails = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (financeDetails == null) {
            throw new RuntimeException("Finance details not found for order ID: " + customerOrderId);
        }
        if (financeDetails.getFinanceStatus() != FinanceStatus.PENDING) {
            throw new IllegalStateException("Finance request is not in PENDING status for order ID: " + customerOrderId);
        }

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + customerOrderId));

        historyService.saveFinanceHistory(financeDetails, rejectedBy, FinanceStatus.REJECTED);
        financeDetails.setFinanceStatus(FinanceStatus.REJECTED);
        financeDetails.setRejectedBy(rejectedBy);
        financeDetailsRepository.save(financeDetails);

        historyService.saveOrderHistory(orderDetails, rejectedBy, OrderStatus.PENDING);
        orderDetails.setOrderStatus(OrderStatus.PENDING);
        vehicleOrderDetailsRepository.save(orderDetails);

        return mapToFinanceResponse(financeDetails, orderDetails);
    }
    @Transactional
    public FinanceResponse updateFinanceDetails(FinanceDTO financeDTO) {
        String customerOrderId = financeDTO.getCustomerOrderId();
        log.info("Updating finance details for order ID: {}", customerOrderId);

        FinanceDetails financeDetails = financeDetailsRepository.findByCustomerOrderId(customerOrderId);
        if (financeDetails == null) {
            throw new RuntimeException("Finance details not found for order ID: " + customerOrderId);
        }

        // Update fields if provided
        if (financeDTO.getCustomerName() != null) {
            financeDetails.setCustomerName(financeDTO.getCustomerName());
        }
        if (financeDTO.getFinanceStatus() != null) {
            financeDetails.setFinanceStatus(FinanceStatus.valueOf(financeDTO.getFinanceStatus()));
        }
        if (financeDTO.getApprovedBy() != null) {
            financeDetails.setApprovedBy(financeDTO.getApprovedBy());
        }
        if (financeDTO.getRejectedBy() != null) {
            financeDetails.setRejectedBy(financeDTO.getRejectedBy());
        }

        financeDetailsRepository.save(financeDetails);

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(customerOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found for customer order ID: " + customerOrderId));

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
        return response;
    }
}