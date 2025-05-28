package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VehicleSalesParentWorkflow {

    @WorkflowMethod
    OrderResponse processOrder(OrderRequest orderRequest);
@SignalMethod
    void initiateFinance(FinanceRequest financeRequest);
@SignalMethod
    void approveFinance(String approvedBy);
@SignalMethod
    void rejectFinance(String rejectedBy);
@SignalMethod
    void initiateDispatch(DispatchRequest dispatchRequest);
@SignalMethod
    void confirmDelivery(DeliveryRequest deliveryRequest);
@SignalMethod
    void cancelOrder(Long customerOrderId);

//    @QueryMethod
//    String getWorkflowStatus();
    @QueryMethod
    OrderResponse getOrderStatus();
    @QueryMethod
    String getWorkflowStatus(); // The missing method

}