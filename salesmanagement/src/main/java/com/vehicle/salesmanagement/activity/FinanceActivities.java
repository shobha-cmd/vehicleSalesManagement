package com.vehicle.salesmanagement.activity;

import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface FinanceActivities {

    FinanceResponse createFinanceDetails(FinanceRequest request);

    FinanceResponse getFinanceDetails(String customerOrderId);

    FinanceResponse approveFinance(String customerOrderId, String approvedBy);

    FinanceResponse rejectFinance(String customerOrderId, String rejectedBy);
}