package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VehicleVariantRequest {

    @NotNull(message = "Model ID cannot be null")
    private Long modelId;

    @NotBlank(message = "Variant cannot be blank")
    @Size(min = 1, max = 50, message = "Variant must be between 1 and 50 characters")
    private String variant;

    @Size(max = 10, message = "Suffix must not exceed 10 characters")
    private String suffix;

    @Size(max = 500, message = "Safety feature must not exceed 500 characters")
    private String safetyFeature;

    @NotBlank(message = "Colour cannot be blank")
    @Size(max = 50, message = "Colour must not exceed 50 characters")
    private String colour;

    @Size(max = 50, message = "Engine colour must not exceed 50 characters")
    private String engineColour;

    @NotBlank(message = "Transmission type cannot be blank")
    @Size(max = 20, message = "Transmission type must not exceed 20 characters")
    private String transmissionType;

    @Size(max = 50, message = "Interior colour must not exceed 50 characters")
    private String interiorColour;

    @NotBlank(message = "Engine capacity cannot be blank")
    @Size(max = 20, message = "Engine capacity must not exceed 20 characters")
    private String engineCapacity;

    @NotBlank(message = "Fuel type cannot be blank")
    @Size(max = 20, message = "Fuel type must not exceed 20 characters")
    private String fuelType;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Year of manufacture cannot be null")
    @Min(value = 1900, message = "Year of manufacture must be after 1900")
    @Max(value = 9999, message = "Year of manufacture must be a valid year")
    private Integer yearOfManufacture;

    @NotBlank(message = "Body type cannot be blank")
    @Size(max = 50, message = "Body type must not exceed 50 characters")
    private String bodyType;

    @NotNull(message = "Fuel tank capacity cannot be null")
    @DecimalMin(value = "0.1", message = "Fuel tank capacity must be greater than 0")
    private BigDecimal fuelTankCapacity;

    @NotNull(message = "Seating capacity cannot be null")
    @Min(value = 1, message = "Seating capacity must be at least 1")
    private Integer seatingCapacity;

    @NotBlank(message = "Max power cannot be blank")
    @Size(max = 50, message = "Max power must not exceed 50 characters")
    private String maxPower;

    @NotBlank(message = "Max torque cannot be blank")
    @Size(max = 50, message = "Max torque must not exceed 50 characters")
    private String maxTorque;

    @NotBlank(message = "Top speed cannot be blank")
    @Size(max = 50, message = "Top speed must not exceed 50 characters")
    private String topSpeed;

    @NotBlank(message = "Wheel base cannot be blank")
    @Size(max = 50, message = "Wheel base must not exceed 50 characters")
    private String wheelBase;

    @NotBlank(message = "Width cannot be blank")
    @Size(max = 50, message = "Width must not exceed 50 characters")
    private String width;

    @NotBlank(message = "Length cannot be blank")
    @Size(max = 50, message = "Length must not exceed 50 characters")
    private String length;

    @Size(max = 500, message = "Infotainment must not exceed 500 characters")
    private String infotainment;

    @Size(max = 500, message = "Comfort must not exceed 500 characters")
    private String comfort;

    @NotNull(message = "Number of air bags cannot be null")
    @Min(value = 0, message = "Number of air bags cannot be negative")
    private Integer numberOfAirBags;

    @NotNull(message = "Mileage city cannot be null")
    @DecimalMin(value = "0.0", message = "Mileage city cannot be negative")
    private BigDecimal mileageCity;

    @NotNull(message = "Mileage highway cannot be null")
    @DecimalMin(value = "0.0", message = "Mileage highway cannot be negative")
    private BigDecimal mileageHighway;
}