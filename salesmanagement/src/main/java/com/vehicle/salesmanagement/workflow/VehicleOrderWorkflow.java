package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;

@WorkflowInterface
public interface VehicleOrderWorkflow {
    @WorkflowMethod
    OrderResponse placeOrder(OrderRequest orderRequest);

    //List<OrderResponse> handleMultipleOrder(List<OrderRequest> orderRequests);

    @SignalMethod
    void cancelOrder(Long orderId);
}