package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DispatchDeliveryWorkflow {

    @WorkflowMethod
    DeliveryResponse processDispatchAndDelivery(DispatchRequest dispatchRequest);

    @SignalMethod
    void confirmDelivery(DeliveryRequest deliveryRequest);

}