package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FinanceRequest {

    @NotNull(message = "Customer order ID cannot be null")
    //@Pattern(regexp = "^TYT-\\d{4}-\\d{3}$", message = "Customer order ID must be in the format TYT-YYYY-NNN")
   // @Size(min = 11, max = 11, message = "Customer order ID must be exactly 11 characters")
    private String customerOrderId;

    @NotBlank(message = "Customer name cannot be blank")
    @Size(min = 1, max = 100, message = "Customer name must be between 1 and 100 characters")
    private String customerName;

    @NotNull(message = "Vehicle model ID cannot be null")
    private Long vehicleModelId;

    @NotNull(message = "Vehicle variant ID cannot be null")
    private Long vehicleVariantId;

    @NotBlank(message = "Model name cannot be blank")
    @Size(min = 1, max = 100, message = "Model name must be between 1 and 100 characters")
    private String modelName;

    @NotBlank(message = "Variant cannot be blank")
    @Size(min = 1, max = 50, message = "Variant must be between 1 and 50 characters")
    private String variant;

    @NotBlank(message = "Colour cannot be blank")
    @Size(min = 1, max = 50, message = "Colour must be between 1 and 50 characters")
    private String colour;

    @NotBlank(message = "Fuel type cannot be blank")
    @Size(min = 1, max = 20, message = "Fuel type must be between 1 and 20 characters")
    private String fuelType;

    @NotBlank(message = "Transmission type cannot be blank")
    @Size(min = 1, max = 20, message = "Transmission type must be between 1 and 20 characters")
    private String transmissionType;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

//    @NotNull(message = "Total price cannot be null")
//    @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
//    private Double totalPrice;
//
//    @NotNull(message = "Booking amount cannot be null")
//    @DecimalMin(value = "0.01", message = "Booking amount must be greater than 0")
//    private Double bookingAmount;

    @NotBlank(message = "Payment mode cannot be blank")
    @Size(min = 1, max = 50, message = "Payment mode must be between 1 and 50 characters")
    private String paymentMode;

    public String getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(String customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public void setFinanceStatus(String approved) {
    }
}