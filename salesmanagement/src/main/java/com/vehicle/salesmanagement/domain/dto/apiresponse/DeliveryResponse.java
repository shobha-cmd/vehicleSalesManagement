package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.DeliveryStatus;
import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryResponse {
    private Long deliveryId;
    private String customerOrderId;
    private String customerName;
    private DeliveryStatus deliveryStatus;
    private OrderStatus orderStatus;
    private String modelName;
    private String variant;
    private LocalDateTime deliveryDate;
    private String deliveredBy;
    private String recipientName;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
}