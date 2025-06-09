package com.vehicle.salesmanagement.domain.dto.apirequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderRequest {

    //@Pattern(regexp = "^TYT-\\d{4}-\\d{3}$", message = "Customer order ID must be in the format TYT-YYYY-NNN")
   // @Size(min = 11, max = 11, message = "Customer order ID must be exactly 11 characters")
    private String customerOrderId;

    @NotNull(message = "Vehicle model ID cannot be null")
    private Long vehicleModelId;

    @NotNull(message = "Vehicle variant ID cannot be null")
    private Long vehicleVariantId;

    @NotBlank(message = "Customer name cannot be blank")
    @Size(min = 1, max = 100, message = "Customer name must be between 1 and 100 characters")
    private String customerName;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 500, message = "Permanent address must not exceed 500 characters")
    private String permanentAddress;

    @Size(max = 500, message = "Current address must not exceed 500 characters")
    private String currentAddress;

    @NotBlank(message = "Aadhar number cannot be blank")
    @Pattern(regexp = "^\\d{12}$", message = "Aadhar number must be exactly 12 numeric digits")
    @Size(min = 12, max = 12, message = "Aadhar number must be exactly 12 digits")
    private String aadharNo;

    //@NotBlank(message = "PAN number cannot be blank")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN number must be in the format ABCDE1234F")
    @Size(min = 10, max = 10, message = "PAN number must be exactly 10 characters")
    private String panNo;

    @NotBlank(message = "Model name cannot be blank")
    @Size(min = 1, max = 100, message = "Model name must be between 1 and 100 characters")
    private String modelName;

    @Size(max = 20, message = "Fuel type must not exceed 20 characters")
    private String fuelType;

    @Size(max = 50, message = "Colour must not exceed 50 characters")
    private String colour;

    @Size(max = 20, message = "Transmission type must not exceed 20 characters")
    private String transmissionType;

    @Size(max = 50, message = "Variant must not exceed 50 characters")
    private String variant;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

//    @NotNull(message = "Total price cannot be null")
//    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
//    private BigDecimal totalPrice;
//
//    @NotNull(message = "Booking amount cannot be null")
//    @DecimalMin(value = "0.01", message = "Booking amount must be greater than 0")
//    private BigDecimal bookingAmount;

    @NotBlank(message = "Payment mode cannot be blank")
    @Size(min = 1, max = 50, message = "Payment mode must be between 1 and 50 characters")
    private String paymentMode;

//    @NotBlank(message = "Created by cannot be blank")
//    @Size(min = 1, max = 100, message = "Created by must be between 1 and 100 characters")
//    private String createdBy;
//
//    @NotBlank(message = "Updated by cannot be blank")
//    @Size(min = 1, max = 100, message = "Created by must be between 1 and 100 characters")
//    private String updatedBy;

    public OrderRequest() {
    }

    @JsonCreator
    public OrderRequest(
            @JsonProperty("customerOrderId") String customerOrderId,
            @JsonProperty("vehicleModelId") Long vehicleModelId,
            @JsonProperty("vehicleVariantId") Long vehicleVariantId,
            @JsonProperty("customerName") String customerName,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("email") String email,
            @JsonProperty("permanentAddress") String permanentAddress,
            @JsonProperty("currentAddress") String currentAddress,
            @JsonProperty("aadharNo") String aadharNo,
            @JsonProperty("panNo") String panNo,
            @JsonProperty("modelName") String modelName,
            @JsonProperty("fuelType") String fuelType,
            @JsonProperty("colour") String colour,
            @JsonProperty("transmissionType") String transmissionType,
            @JsonProperty("variant") String variant,
            @JsonProperty("quantity") Integer quantity,
            // @JsonProperty("totalPrice") BigDecimal totalPrice,
            // @JsonProperty("bookingAmount") BigDecimal bookingAmount,
            @JsonProperty("paymentMode") String paymentMode) {
        // @JsonProperty("createdBy") String createdBy,
        // @JsonProperty("updatedBy") String updatedBy) {
        this.customerOrderId = customerOrderId;
        this.vehicleModelId = vehicleModelId;
        this.vehicleVariantId = vehicleVariantId;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.permanentAddress = permanentAddress;
        this.currentAddress = currentAddress;
        this.aadharNo = aadharNo;
        this.panNo = panNo;
        this.modelName = modelName;
        this.fuelType = fuelType;
        this.colour = colour;
        this.transmissionType = transmissionType;
        this.variant = variant;
        this.quantity = quantity;
        // this.totalPrice = totalPrice;
        // this.bookingAmount = bookingAmount;
        this.paymentMode = paymentMode;
        // this.createdBy = createdBy;
        // this.updatedBy = updatedBy;
    }

    public OrderRequest(Long vehicleModelId, Long vehicleVariantId, @Size(min = 1, max = 100, message = "Customer name must be between 1 and 100 characters") String customerName, @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits") @Size(min = 10, max = 10, message = "Phone number must be exactly 10 digits") String phoneNumber, @Email(message = "Email must be a valid email address") @Size(max = 100, message = "Email must not exceed 100 characters") String email, @Pattern(regexp = "^\\d{12}$", message = "Aadhar number must be exactly 12 numeric digits") @Size(min = 12, max = 12, message = "Aadhar number must be exactly 12 digits") String aadharNo, @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN number must be in the format ABCDE1234F") @Size(min = 10, max = 10, message = "PAN number must be exactly 10 characters") String panNo, @Size(min = 1, max = 100, message = "Model name must be between 1 and 100 characters") String modelName, @Min(value = 1, message = "Quantity must be at least 1") Integer quantity, @Size(min = 1, max = 50, message = "Payment mode must be between 1 and 50 characters") String paymentMode) {
    }
}