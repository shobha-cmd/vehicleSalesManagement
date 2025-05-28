package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VehicleCancelWorkflow {
    @WorkflowMethod
    OrderResponse startCancelOrder(Long customerOrderId);
}