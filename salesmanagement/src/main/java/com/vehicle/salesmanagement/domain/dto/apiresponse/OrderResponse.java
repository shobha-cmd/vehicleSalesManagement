package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class OrderResponse {

    private Long customerOrderId;
    private Long vehicleModelId;
    private Long vehicleVariantId;
    private String customerName;
    private String phoneNumber;
    private String email;
    private String permanentAddress;
    private String currentAddress;
    private String aadharNo;
    private String panNo;
    private String modelName;
    private String fuelType;
    private String colour;
    private String transmissionType;
    private String variant;
    private Integer quantity;
//    private BigDecimal totalPrice;
//    private BigDecimal bookingAmount;
    private String paymentMode;
    @NonNull
    private OrderStatus orderStatus;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//  private String createdBy;
//    private String updatedBy;
    private String message;

    // Add custom constructor
    public OrderResponse(Long customerOrderId, OrderStatus orderStatus) {
        this.customerOrderId = customerOrderId;
        this.orderStatus = orderStatus;
    }
}