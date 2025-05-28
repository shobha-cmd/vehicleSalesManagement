//package com.vehicle.salesmanagement.domain.dto.apirequest;
//
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Pattern;
//import lombok.Data;
//
//@Data
//public class StockAddRequest {
//    @NotNull(message = "Model ID cannot be null")
//    private Long modelId;
//
//    @NotNull(message = "Variant ID cannot be null")
//    private Long variantId;
//
//    @NotBlank(message = "Suffix cannot be blank")
//    private String suffix;
//
//    @NotBlank(message = "Fuel type cannot be blank")
//    private String fuelType;
//
//    @NotBlank(message = "Colour cannot be blank")
//    private String colour;
//
//    @NotBlank(message = "Engine colour cannot be blank")
//    private String engineColour;
//
//    @NotBlank(message = "Transmission type cannot be blank")
//    private String transmissionType;
//
//    @NotBlank(message = "Variant name cannot be blank")
//    private String variantName;
//
//    private Integer quantity;
//
//    @NotBlank(message = "Interior colour cannot be blank")
//    private String interiorColour;
//
//    @NotBlank(message = "VIN number cannot be blank")
//    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN number must be 17 alphanumeric characters (excluding I, O, Q)")
//    private String vinNumber;
//
//    private String createdBy;
//    private String updatedBy;
//}