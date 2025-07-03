package com.vehicle.salesmanagement.domain.dto.apirequest;

import lombok.Data;

@Data
public class FinanceDTO {
    private Long financeId;

   // @Pattern(regexp = "^TYT-\\d{4}-\\d{3}$", message = "Customer order ID must be in the format TYT-YYYY-NNN")
    //@Size(min = 11, max = 11, message = "Customer order ID must be exactly 11 characters")
    private String customerOrderId;

    private String customerName;
    private String financeStatus;
    private String approvedBy;
    private String rejectedBy;

    public Long getFinanceId() {
        return financeId;
    }

    public void setFinanceId(Long financeId) {
        this.financeId = financeId;
    }
}