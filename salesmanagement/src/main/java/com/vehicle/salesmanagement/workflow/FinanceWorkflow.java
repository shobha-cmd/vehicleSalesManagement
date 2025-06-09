package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FinanceWorkflow {

    @WorkflowMethod
    FinanceResponse processFinance(FinanceRequest financeRequest);

    @SignalMethod
    void approveFinance(String approvedBy);

    @SignalMethod
    void rejectFinance(String rejectedBy);

//    @QueryMethod
//    String getWorkflowStatus();
}