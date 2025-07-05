package com.vehicle.salesmanagement.domain.entity.model;

import com.vehicle.salesmanagement.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "vehicle_order_details",schema="sales_tracking")
@AllArgsConstructor
@NoArgsConstructor
public class VehicleOrderDetails {

    @Id
    @Column(name = "customer_order_id", length = 20)
    private String customerOrderId;

    @ManyToOne
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    @NotNull(message = "Vehicle model is required")
    private VehicleModel vehicleModelId;

    @ManyToOne
    @JoinColumn(name = "vehicle_variant_id", nullable = false)
    @NotNull(message = "Vehicle variant is required")
    private VehicleVariant vehicleVariantId;

    @Column(name = "customer_name", length = 100, nullable = false)
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Column(name = "phone_number", length = 15, nullable = false)
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Column(name = "email", length = 100, nullable = false)
    @NotBlank(message = "Email is required")
    private String email;

    @Column(name = "permanent_address", columnDefinition = "TEXT")
    private String permanentAddress;

    @Column(name = "current_address", columnDefinition = "TEXT")
    private String currentAddress;

    @Column(name = "aadhar_no", length = 20, nullable = false)
    @NotBlank(message = "Aadhar number is required")
    private String aadharNo;

    @Column(name = "pan_no", length = 20, nullable = false)
    @NotBlank(message = "PAN number is required")
    private String panNo;

    @Column(name = "model_name", length = 100, nullable = false)
    @NotBlank(message = "Model name is required")
    private String modelName;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Column(name = "colour", length = 50)
    private String colour;

    @Column(name = "transmission_type", length = 50)
    private String transmissionType;

    @Column(name = "variant", length = 50)
    private String variant;

    @Column(name = "quantity", nullable = false)
    @NotNull(message = "Quantity is required")
    private Integer quantity;

//    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
//    //@NotNull(message = "Total price is required")
//    private BigDecimal totalPrice;

//    @Column(name = "booking_amount", precision = 15, scale = 2, nullable = false)
//    //@NotNull(message = "Booking amount is required")
//    private BigDecimal bookingAmount;

    @Column(name = "payment_mode", length = 50, nullable = false)
    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Column(name = "expected_delivery_date")
    private String expectedDeliveryDate;

//    @Column(name = "created_at", nullable = false)
//   // @NotNull(message = "Created at is required")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at", nullable = false)
//    //@NotNull(message = "Updated at is required")
//    private LocalDateTime updatedAt;
//
//    @Column(name = "created_by", length = 100, nullable = false)
//    //@NotBlank(message = "Created by is required")
//    private String createdBy;
//
//    @Column(name = "updated_by", length = 100, nullable = false)
//    //@NotBlank(message = "Updated by is required")
//    private String updatedBy;
}