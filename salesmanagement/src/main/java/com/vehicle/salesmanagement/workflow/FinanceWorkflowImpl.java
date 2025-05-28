package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.activity.FinanceActivities;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.enums.FinanceStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class FinanceWorkflowImpl implements FinanceWorkflow {

    private final FinanceActivities activities;
    private boolean isApproved = false;
    private boolean isRejected = false;
    private String approvedBy;
    private String rejectedBy;

    public FinanceWorkflowImpl() {
        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build();
        this.activities = Workflow.newActivityStub(FinanceActivities.class, options);
    }

    @Override
    public FinanceResponse processFinance(FinanceRequest financeRequest) {
        log.info("Starting finance workflow for order ID: {}", financeRequest.getCustomerOrderId());

        // Retrieve existing finance details
        FinanceResponse response;
        try {
            response = activities.getFinanceDetails(financeRequest.getCustomerOrderId());
            if (response.getFinanceStatus() != FinanceStatus.PENDING) {
                log.warn("Finance details for order ID: {} are not in PENDING status: {}",
                        financeRequest.getCustomerOrderId(), response.getFinanceStatus());
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve finance details for order ID: {}: {}",
                    financeRequest.getCustomerOrderId(), e.getMessage());
            throw new RuntimeException("Failed to retrieve finance details: " + e.getMessage(), e);
        }

        // Wait for approval or rejection signal
        Workflow.await(Duration.ofDays(7), () -> isApproved || isRejected);

        if (isApproved) {
            log.info("Finance approved for order ID: {}", financeRequest.getCustomerOrderId());
            response = activities.approveFinance(financeRequest.getCustomerOrderId(), approvedBy);
            response.setOrderStatus(OrderStatus.ALLOTTED);
        } else if (isRejected) {
            log.info("Finance rejected for order ID: {}", financeRequest.getCustomerOrderId());
            response = activities.rejectFinance(financeRequest.getCustomerOrderId(), rejectedBy);
            response.setOrderStatus(OrderStatus.PENDING);
        } else {
            log.warn("Finance workflow timed out for order ID: {}", financeRequest.getCustomerOrderId());
            response = activities.rejectFinance(financeRequest.getCustomerOrderId(), "SYSTEM_TIMEOUT");
            response.setFinanceStatus(FinanceStatus.REJECTED);
            response.setOrderStatus(OrderStatus.PENDING);
        }

        log.info("Finance workflow completed for order ID: {} with finance status: {}, order status: {}",
                financeRequest.getCustomerOrderId(), response.getFinanceStatus(), response.getOrderStatus());
        return response;
    }

    @Override
    public void approveFinance(String approvedBy) {
        log.info("Received approval signal for finance with approvedBy: {}", approvedBy);
        this.approvedBy = approvedBy;
        this.isApproved = true;
    }

    @Override
    public void rejectFinance(String rejectedBy) {
        log.info("Received rejection signal for finance with rejectedBy: {}", rejectedBy);
        this.rejectedBy = rejectedBy;
        this.isRejected = true;
    }
}