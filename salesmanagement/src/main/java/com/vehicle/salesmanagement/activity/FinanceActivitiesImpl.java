package com.vehicle.salesmanagement.activity;

import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.service.FinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceActivitiesImpl implements FinanceActivities {

    private final FinanceService financeService;

    @Override
    public FinanceResponse createFinanceDetails(FinanceRequest request) {
        try {
            log.info("Activity: Creating finance details for order ID: {}", request.getCustomerOrderId());
            return financeService.createFinanceDetails(request);
        } catch (Exception e) {
            log.error("Failed to create finance details: {}", e.getMessage());
            throw new RuntimeException("Failed to create finance details: " + e.getMessage(), e);
        }
    }

    @Override
    public FinanceResponse getFinanceDetails(Long customerOrderId) {
        try {
            log.info("Activity: Retrieving finance details for order ID: {}", customerOrderId);
            return financeService.getFinanceDetails(customerOrderId);
        } catch (Exception e) {
            log.error("Failed to retrieve finance details: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve finance details: " + e.getMessage(), e);
        }
    }

    @Override
    public FinanceResponse approveFinance(Long customerOrderId, String approvedBy) {
        try {
            log.info("Activity: Approving finance for order ID: {}", customerOrderId);
            return financeService.approveFinance(customerOrderId, approvedBy);
        } catch (Exception e) {
            log.error("Failed to approve finance: {}", e.getMessage());
            throw new RuntimeException("Failed to approve finance: " + e.getMessage(), e);
        }
    }

    @Override
    public FinanceResponse rejectFinance(Long customerOrderId, String rejectedBy) {
        try {
            log.info("Activity: Rejecting finance for order ID: {}", customerOrderId);
            return financeService.rejectFinance(customerOrderId, rejectedBy);
        } catch (Exception e) {
            log.error("Failed to reject finance: {}", e.getMessage());
            throw new RuntimeException("Failed to reject finance: " + e.getMessage(), e);
        }
    }
}