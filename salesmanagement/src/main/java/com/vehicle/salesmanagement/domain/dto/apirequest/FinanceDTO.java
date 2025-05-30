package com.vehicle.salesmanagement.domain.dto.apirequest;

import lombok.Data;

@Data
public class FinanceDTO {
    private Long financeId;
    private Long customerOrderId;
    private String customerName;
    private String financeStatus;
    private String approvedBy;
    private String rejectedBy;
}