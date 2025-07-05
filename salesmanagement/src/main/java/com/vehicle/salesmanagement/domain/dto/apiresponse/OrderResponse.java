package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class OrderResponse {

    private String customerOrderId;
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
//    private String createdBy;
//    private String updatedBy;
    private String message;
    private String expectedDeliveryDate;

    // Add custom constructor
    public OrderResponse(String customerOrderId, OrderStatus orderStatus) {
        this.customerOrderId = customerOrderId;
        this.orderStatus = orderStatus;
    }
}