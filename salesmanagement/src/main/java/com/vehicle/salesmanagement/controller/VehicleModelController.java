package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apiresponse.*;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.service.VehicleModelService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Vehicle Model Management", description = "Endpoints for managing vehicle models and variants")
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;
    private final ObjectMapper objectMapper;

    @GetMapping("/dropdown-data")
    @Operation(summary = "Fetch dropdown data", description = "Fetches all data required for dropdowns including models, variants, fuel types, colors, etc., optionally filtered by model name and variant")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dropdown data retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid model name or variant provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> getDropdownData(
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "variant", required = false) String variant) {
        log.info("Received request to fetch dropdown data with modelName: {}, variant: {}", modelName, variant);
        try {
            if (modelName != null && modelName.trim().isEmpty()) {
                log.error("Model name cannot be empty when provided");
                ApiResponse apiResponse = new ApiResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Model name cannot be empty",
                        null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }
            if (variant != null && variant.trim().isEmpty()) {
                log.error("Variant cannot be empty when provided");
                ApiResponse apiResponse = new ApiResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Variant cannot be empty",
                        null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }

            VehicleAttributesResponse response = vehicleModelService.getDropdownData(modelName, variant);
            String message = buildResponseMessage(modelName, variant);
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.OK.value(),
                    message,
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (NumberFormatException e) {
            log.error("Invalid numeric data provided: {}", e.getMessage());
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid numeric data: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error retrieving dropdown data: {}", e.getMessage());
            ApiResponse apiResponse = new ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    private String buildResponseMessage(String modelName, String variant) {
        if (modelName != null && variant != null) {
            return "Dropdown data for model " + modelName + " and variant " + variant + " retrieved successfully";
        } else if (modelName != null) {
            return "Dropdown data for model " + modelName + " retrieved successfully";
        } else {
            return "All dropdown data retrieved successfully";
        }
    }

    @PostMapping("/vehiclemodels/save")
    @Operation(summary = "Save vehicle model(s)", description = "Saves one or multiple vehicle models. Examples show field data types.")
    @RequestBody(
            description = "Vehicle model(s) to save. Each model includes modelName (string), createdBy (string), and updatedBy (string). Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { VehicleModelDTO.class, VehicleModelDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Model Example",
                                    summary = "Data type example for a single vehicle model",
                                    value = "{\"modelName\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Models Example",
                                    summary = "Data type example for multiple vehicle models",
                                    value = "[{\"modelName\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"modelName\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<?> saveVehicleModels(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to save vehicle models at {}", java.time.LocalDateTime.now());
        try {
            if (request instanceof List) {
                List<VehicleModelDTO> dtos = objectMapper.convertValue(request,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, VehicleModelDTO.class));
                log.info("Processing {} vehicle models", dtos.size());
                List<VehicleModel> savedModels = vehicleModelService.saveVehicleModels(dtos);
                log.info("Successfully saved {} vehicle models", savedModels.size());
                return ResponseEntity.ok(savedModels);
            } else {
                VehicleModelDTO dto = objectMapper.convertValue(request, VehicleModelDTO.class);
                log.info("Processing single vehicle model");
                VehicleModel savedModel = vehicleModelService.saveVehicleModel(dto);
                log.info("Successfully saved vehicle model: {}", savedModel.getModelName());
                return ResponseEntity.ok(Collections.singletonList(savedModel));
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error saving vehicle models: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving vehicle models: " + e.getMessage());
        }
    }

    @PostMapping("/vehiclevariants/save")
    @Operation(summary = "Save vehicle variant(s)", description = "Saves one or multiple vehicle variants. Examples show field data types.")
    @RequestBody(
            description = "Vehicle variant(s) to save. Includes fields like vehicleModelId (integer), variant (string), price (number), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { VehicleVariantDTO.class, VehicleVariantDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Variant Example",
                                    summary = "Data type example for a single vehicle variant",
                                    value = "{\"vehicleModelId\": \"integer\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Variants Example",
                                    summary = "Data type example for multiple vehicle variants",
                                    value = "[{\"vehicleModelId\": \"integer\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<?> saveVehicleVariants(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to save vehicle variants at {}", java.time.LocalDateTime.now());
        try {
            if (request instanceof List) {
                List<VehicleVariantDTO> dtos = objectMapper.convertValue(request,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, VehicleVariantDTO.class));
                log.info("Processing {} vehicle variants", dtos.size());
                List<VehicleVariant> savedVariants = vehicleModelService.saveVehicleVariants(dtos);
                log.info("Successfully saved {} vehicle variants", savedVariants.size());
                return ResponseEntity.ok(savedVariants);
            } else {
                VehicleVariantDTO dto = objectMapper.convertValue(request, VehicleVariantDTO.class);
                log.info("Processing single vehicle variant");
                VehicleVariant savedVariant = vehicleModelService.saveVehicleVariant(dto);
                log.info("Successfully saved vehicle variant: {}", savedVariant.getVariant());
                return ResponseEntity.ok(Collections.singletonList(savedVariant));
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error saving vehicle variants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving vehicle variants: " + e.getMessage());
        }
    }

    @PostMapping("/stockdetails/save")
    @Operation(summary = "Save stock detail(s)", description = "Saves one or multiple stock details. Examples show field data types.")
    @RequestBody(
            description = "Stock detail(s) to save. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), quantity (integer), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { StockDetailsDTO.class, StockDetailsDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Stock Example",
                                    summary = "Data type example for a single stock detail",
                                    value = "{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Stock Example",
                                    summary = "Data type example for multiple stock details",
                                    value = "[{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<?> saveStockDetails(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to save stock details at {}", java.time.LocalDateTime.now());
        try {
            if (request instanceof List) {
                List<StockDetailsDTO> dtos = objectMapper.convertValue(request,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, StockDetailsDTO.class));
                log.info("Processing {} stock entries", dtos.size());
                List<StockDetails> savedStock = vehicleModelService.saveStockDetails(dtos);
                log.info("Successfully saved {} stock entries", savedStock.size());
                return ResponseEntity.ok(savedStock);
            } else {
                StockDetailsDTO dto = objectMapper.convertValue(request, StockDetailsDTO.class);
                log.info("Processing single stock detail");
                StockDetails savedStock = vehicleModelService.saveStockDetail(dto);
                log.info("Successfully saved stock detail for VIN: {}", savedStock.getVinNumber());
                return ResponseEntity.ok(Collections.singletonList(savedStock));
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid stock request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error saving stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving stock details: " + e.getMessage());
        }
    }

    @PostMapping("/mddpstock/save")
    @Operation(summary = "Save MDDP stock(s)", description = "Saves one or multiple MDDP stock entries. Examples show field data types.")
    @RequestBody(
            description = "MDDP stock(s) to save. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), expectedDispatchDate (string), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { MddpStockDTO.class, MddpStockDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single MDDP Stock Example",
                                    summary = "Data type example for a single MDDP stock entry",
                                    value = "{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple MDDP Stock Example",
                                    summary = "Data type example for multiple MDDP stock entries",
                                    value = "[{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<?> saveMddpStock(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to save MDDP stock at {}", java.time.LocalDateTime.now());
        try {
            if (request instanceof List) {
                List<MddpStockDTO> dtos = objectMapper.convertValue(request,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, MddpStockDTO.class));
                log.info("Processing {} MDDP stock entries", dtos.size());
                List<MddpStock> savedMddpStock = vehicleModelService.saveMddpStock(dtos);
                log.info("Successfully saved {} MDDP stock entries", savedMddpStock.size());
                return ResponseEntity.ok(savedMddpStock);
            } else {
                MddpStockDTO dto = objectMapper.convertValue(request, MddpStockDTO.class);
                log.info("Processing single MDDP stock");
                MddpStock savedMddpStock = vehicleModelService.saveMddpStock(dto);
                log.info("Successfully saved MDDP stock for VIN: {}", savedMddpStock.getVinNumber());
                return ResponseEntity.ok(Collections.singletonList(savedMddpStock));
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid MDDP stock request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error saving MDDP stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving MDDP stock: " + e.getMessage());
        }
    }

    @PostMapping("/manufacturerorders/save")
    @Operation(summary = "Save manufacturer order(s)", description = "Saves one or multiple manufacturer orders. Examples show field data types.")
    @RequestBody(
            description = "Manufacturer order(s) to save. Includes fields like vehicleVariantId (integer), manufacturerLocation (string), estimatedArrivalDate (string), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { ManufacturerOrderDTO.class, ManufacturerOrderDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Manufacturer Order Example",
                                    summary = "Data type example for a single manufacturer order",
                                    value = "{\"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Manufacturer Orders Example",
                                    summary = "Data type example for multiple manufacturer orders",
                                    value = "[{\"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<?> saveManufacturerOrders(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to save manufacturer orders at {}", java.time.LocalDateTime.now());
        try {
            if (request instanceof List) {
                List<ManufacturerOrderDTO> dtos = objectMapper.convertValue(request,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ManufacturerOrderDTO.class));
                log.info("Processing {} manufacturer order entries", dtos.size());
                List<ManufacturerOrder> savedOrders = vehicleModelService.saveManufacturerOrders(dtos);
                log.info("Successfully saved {} manufacturer order entries", savedOrders.size());
                return ResponseEntity.ok(savedOrders);
            } else {
                ManufacturerOrderDTO dto = objectMapper.convertValue(request, ManufacturerOrderDTO.class);
                log.info("Processing single manufacturer order");
                ManufacturerOrder savedOrder = vehicleModelService.saveManufacturerOrder(dto);
                log.info("Successfully saved manufacturer order with ID: {}", savedOrder.getManufacturerId());
                return ResponseEntity.ok(Collections.singletonList(savedOrder));
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid manufacturer order request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error saving manufacturer orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving manufacturer orders: " + e.getMessage());
        }
    }
}