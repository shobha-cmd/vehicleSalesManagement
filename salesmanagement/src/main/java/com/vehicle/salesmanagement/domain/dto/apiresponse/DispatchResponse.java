package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.DispatchStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DispatchResponse {
    private Long dispatchId;
    private Long customerOrderId;
    private String customerName;
    private DispatchStatus dispatchStatus;
    private OrderStatus orderStatus;
    private String modelName;
    private String variant;
    private LocalDateTime dispatchDate;
    private String dispatchedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}