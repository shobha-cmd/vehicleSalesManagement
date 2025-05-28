package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.activity.VehicleOrderActivities;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class VehicleCancelWorkflowImpl implements VehicleCancelWorkflow {

    private final VehicleOrderActivities activities;

    public VehicleCancelWorkflowImpl() {
        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build();
        this.activities = Workflow.newActivityStub(VehicleOrderActivities.class, options);
    }

    @Override
    public OrderResponse startCancelOrder(Long customerOrderId) {
        log.info("Starting cancellation workflow for order ID: {}", customerOrderId);
        OrderResponse response = activities.cancelOrder(customerOrderId);
        log.info("Cancellation workflow completed for order ID: {}", customerOrderId);
        return response;
    }
}