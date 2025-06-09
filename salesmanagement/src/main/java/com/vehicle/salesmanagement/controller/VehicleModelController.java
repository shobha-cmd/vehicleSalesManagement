package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoGridResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleAttributesResponse;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.service.VehicleModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Vehicle Model Management", description = "Endpoints for managing vehicle models and variants")
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;
    private final ObjectMapper objectMapper;

    private <T> List<T> normalizeToList(Object payload, Class<T> clazz) {
        if (payload instanceof List<?>) {
            return ((List<?>) payload).stream()
                    .map(item -> objectMapper.convertValue(item, clazz))
                    .collect(Collectors.toList());
        } else {
            return List.of(objectMapper.convertValue(payload, clazz));
        }
    }

    @GetMapping("/dropdownData")
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
            @RequestParam(value = "variant", required = false) String variant,
            @RequestParam(value = "vehicleModelId", required = false) Long vehicleModelId,
            @RequestParam(value = "vehicleVariantId", required = false) Long vehicleVariantId) {
        log.info("Received request to fetch dropdown data with modelName: {}, variant: {}, vehicleModelId: {}, vehicleVariantId: {}",
                modelName, variant, vehicleModelId, vehicleVariantId);
        try {
            // Validate input parameters
            if (modelName != null && modelName.trim().isEmpty()) {
                log.error("Model name cannot be empty when provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Model name cannot be empty", null));
            }
            if (variant != null && variant.trim().isEmpty()) {
                log.error("Variant cannot be empty when provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Variant cannot be empty", null));
            }
            if (vehicleModelId != null && vehicleModelId <= 0) {
                log.error("Vehicle model ID must be positive when provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Vehicle model ID must be positive", null));
            }
            if (vehicleVariantId != null && vehicleVariantId <= 0) {
                log.error("Vehicle variant ID must be positive when provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Vehicle variant ID must be positive", null));
            }

            // Fetch dropdown data with IDs
            VehicleAttributesResponse response = vehicleModelService.getDropdownData(modelName, variant, vehicleModelId, vehicleVariantId);
            String message = buildResponseMessage(modelName, variant, vehicleModelId, vehicleVariantId);
            return ResponseEntity.ok(new ApiResponse(HttpStatus.OK.value(), message, response));
        } catch (NumberFormatException e) {
            log.error("Invalid numeric data provided: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(HttpStatus.BAD_REQUEST.value(), "Invalid numeric data: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving dropdown data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + e.getMessage(), null));
        }
    }

    private String buildResponseMessage(String modelName, String variant, Long vehicleModelId, Long vehicleVariantId) {
        StringBuilder message = new StringBuilder("Dropdown data");
        boolean hasFilter = false;

        if (modelName != null) {
            message.append(" for model ").append(modelName);
            hasFilter = true;
        }
        if (variant != null) {
            message.append(hasFilter ? " and variant " : " for variant ").append(variant);
            hasFilter = true;
        }
        if (vehicleModelId != null) {
            message.append(hasFilter ? " and model ID " : " for model ID ").append(vehicleModelId);
            hasFilter = true;
        }
        if (vehicleVariantId != null) {
            message.append(hasFilter ? " and variant ID " : " for variant ID ").append(vehicleVariantId);
            hasFilter = true;
        }

        message.append(hasFilter ? " retrieved successfully" : "All dropdown data retrieved successfully");
        return message.toString();
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
    public ResponseEntity<KendoGridResponse<VehicleModel>> saveVehicleModels(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to save vehicle models at {}", java.time.LocalDateTime.now());
        try {
            List<VehicleModelDTO> dtos = normalizeToList(request, VehicleModelDTO.class);
            log.info("Processing {} vehicle models", dtos.size());
            List<VehicleModel> savedModels = vehicleModelService.saveVehicleModels(dtos).getData(); // Extract data from KendoGridResponse
            log.info("Successfully saved {} vehicle models", savedModels.size());
            return ResponseEntity.ok(new KendoGridResponse<VehicleModel>(savedModels, (long) savedModels.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<VehicleModel>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving vehicle models: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<VehicleModel>(Collections.emptyList(), 0L, "Error saving vehicle models: " + e.getMessage(), null));
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
                                    value = "{\"vehicleModelId\": \"integer\", \"modelName\":\"string\",\"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Variants Example",
                                    summary = "Data type example for multiple vehicle variants",
                                    value = "[{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<VehicleVariant>> saveVehicleVariants(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to save vehicle variants at {}", java.time.LocalDateTime.now());
        try {
            List<VehicleVariantDTO> dtos = normalizeToList(request, VehicleVariantDTO.class);
            log.info("Processing {} vehicle variants", dtos.size());
            KendoGridResponse<VehicleVariant> serviceResponse = vehicleModelService.saveVehicleVariants(dtos);
            List<VehicleVariant> savedVariants = serviceResponse.getData(); // Extract data
            log.info("Successfully saved {} vehicle variants", savedVariants.size());
            return ResponseEntity.ok(new KendoGridResponse<VehicleVariant>(savedVariants, (long) savedVariants.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<VehicleVariant>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving vehicle variants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<VehicleVariant>(Collections.emptyList(), 0L, "Error saving vehicle variants: " + e.getMessage(), null));
        }
    }
    @PostMapping("/stockdetails/save")
    @Operation(summary = "Save stock detail(s)", description = "Saves one or multiple stock details. Examples show field data types.")
    @RequestBody(
            description = "Stock detail(s) to save. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), vinNumber (string), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { StockDetailsDTO.class, StockDetailsDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Stock Example",
                                    summary = "Data type example for a single stock detail",
                                    value = "{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Stock Example",
                                    summary = "Data type example for multiple stock details",
                                    value = "[{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<StockDetails>> saveStockDetails(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to save stock details at {}", java.time.LocalDateTime.now());
        try {
            List<StockDetailsDTO> dtos = normalizeToList(request, StockDetailsDTO.class);
            log.info("Processing {} entries", dtos.size());
            KendoGridResponse<StockDetails> serviceResponse = vehicleModelService.saveStockDetails(dtos);
            List<StockDetails> savedStock = serviceResponse.getData(); // Extract data
            log.info("Successfully saved {} stock details", savedStock.size());
            return ResponseEntity.ok(new KendoGridResponse<StockDetails>(savedStock, (long) savedStock.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid stock request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<StockDetails>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<StockDetails>(Collections.emptyList(), 0L, "Error saving stock details: " + e.getMessage(), null));
        }
    }
    @PostMapping("/mddpstock/save")
    @Operation(summary = "Save MDDP stock detail(s)", description = "Saves one or multiple MDDP stock details. Examples show field data types.")
    @RequestBody(
            description = "MDDP stock detail(s) to save. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), vinNumber (string), expectedDispatchDate (string, ISO format), expectedDeliveryDate (string, ISO format), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { MddpStockDTO.class, MddpStockDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single MDDP Stock Example",
                                    summary = "Data type example for a single MDDP stock detail",
                                    value = "{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple MDDP Stock Example",
                                    summary = "Data type example for multiple MDDP stock details",
                                    value = "[{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<MddpStock>> saveMddpStock(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to save MDDP stock at {}", java.time.LocalDateTime.now());
        try {
            List<MddpStockDTO> dtos = normalizeToList(request, MddpStockDTO.class);
            log.info("Processing {} MDDP stock entries", dtos.size());
            KendoGridResponse<MddpStock> serviceResponse = vehicleModelService.saveMddpStock(dtos);
            List<MddpStock> savedMddpStock = serviceResponse.getData(); // Extract data
            log.info("Successfully saved {} MDDP stock entries", savedMddpStock.size());
            return ResponseEntity.ok(new KendoGridResponse<MddpStock>(savedMddpStock, (long) savedMddpStock.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid MDDP stock request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<MddpStock>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving MDDP stock: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<MddpStock>(Collections.emptyList(), 0L, "Error saving MDDP stock: " + e.getMessage(), null));
        }
    }

    @PostMapping("/manufacturerorders/save")
    @Operation(summary = "Save manufacturer order(s)", description = "Saves one or multiple manufacturer orders. Examples show field data types.")
    @RequestBody(
            description = "Manufacturer order(s) to save. Includes fields like vehicleVariantId (integer), manufacturerLocation (string), orderStatus (string), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { ManufacturerOrderDTO.class, ManufacturerOrderDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Order Example",
                                    summary = "Data type example for a single manufacturer order",
                                    value = "{\"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"2025-12-31T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Orders Example",
                                    summary = "Data type example for multiple manufacturer orders",
                                    value = "[{\"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"2025-12-31T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"2025-12-31T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<ManufacturerOrder>> saveManufacturerOrders(@Valid @org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to save manufacturer orders at {}", java.time.LocalDateTime.now());
        try {
            List<ManufacturerOrderDTO> dtos = normalizeToList(request, ManufacturerOrderDTO.class);
            log.info("Processing {} manufacturer order entries", dtos.size());
            KendoGridResponse<ManufacturerOrder> response = vehicleModelService.saveManufacturerOrders(dtos);
            log.info("Successfully saved {} manufacturer order entries", response.getTotal());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid manufacturer order request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<ManufacturerOrder>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving manufacturer orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<ManufacturerOrder>(Collections.emptyList(), 0L, "Error saving manufacturer orders: " + e.getMessage(), null));
        }
    }
    @PutMapping("/stockdetails/update")
    @Operation(summary = "Update stock detail(s)", description = "Updates one or multiple stock details. Examples show field data types.")
    @RequestBody(
            description = "Stock detail(s) to update. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), vinNumber (string), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { StockDetailsDTO.class, StockDetailsDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Stock Update Example",
                                    summary = "Data type example for a single stock detail update",
                                    value = "{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Stock Update Example",
                                    summary = "Data type example for multiple stock details update",
                                    value = "[{\"vehicleModelId\": \"integer\",\"modelName\":\"string\",\"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock details updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<StockDetails>> updateStockDetails(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to update stock details at {}", java.time.LocalDateTime.now());
        try {
            List<StockDetailsDTO> dtos = normalizeToList(request, StockDetailsDTO.class);
            log.info("Processing update for {} stock entries", dtos.size());
            KendoGridResponse<StockDetails> serviceResponse = vehicleModelService.updateStockDetails(dtos);
            List<StockDetails> updatedStock = serviceResponse.getData();
            log.info("Successfully updated {} stock details", updatedStock.size());
            return ResponseEntity.ok(new KendoGridResponse<>(updatedStock, (long) updatedStock.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid stock update request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error updating stock details: " + e.getMessage(), null));
        }
    }
    @PutMapping("/mddpstock/update")
    @Operation(summary = "Update MDDP stock detail(s)", description = "Updates one or multiple MDDP stock details. Examples show field data types.")
    @RequestBody(
            description = "MDDP stock detail(s) to update. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), vinNumber (string), expectedDispatchDate (string, ISO format), expectedDeliveryDate (string, ISO format), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { MddpStockDTO.class, MddpStockDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single MDDP Stock Update Example",
                                    summary = "Data type example for a single MDDP stock detail update",
                                    value = "{\"vehicleModelId\": \"integer\",\"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple MDDP Stock Update Example",
                                    summary = "Data type example for multiple MDDP stock details update",
                                    value = "[{\"vehicleModelId\": \"integer\",\"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MDDP stock details updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<MddpStock>> updateMddpStock(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to update MDDP stock details at {}", java.time.LocalDateTime.now());
        try {
            List<MddpStockDTO> dtos = normalizeToList(request, MddpStockDTO.class);
            log.info("Processing update for {} MDDP stock entries", dtos.size());
            KendoGridResponse<MddpStock> serviceResponse = vehicleModelService.updateMddpStock(dtos);
            List<MddpStock> updatedMddpStock = serviceResponse.getData();
            log.info("Successfully updated {} MDDP stock details", updatedMddpStock.size());
            return ResponseEntity.ok(new KendoGridResponse<>(updatedMddpStock, (long) updatedMddpStock.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid MDDP stock update request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating MDDP stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error updating MDDP stock details: " + e.getMessage(), null));
        }

    }
    @GetMapping("/stockdetails")
    @Operation(summary = "Get all stock details", description = "Returns stock information including variant, color, quantity, VIN, fuel type, etc.")
    public ResponseEntity<KendoGridResponse<StockDetailsDTO>> getAllStockDetails() {
        List<StockDetailsDTO> stockDTOs = vehicleModelService.getAllStockDetails();
        return ResponseEntity.ok(new KendoGridResponse<>(stockDTOs, stockDTOs.size(), null, null));
    }
    @GetMapping("/mddpstock")
    @Operation(summary = "Get all MDDP stock entries", description = "Returns MDDP stock data compatible with Kendo Grid")
    public ResponseEntity<KendoGridResponse<MddpStockDTO>> getAllMddpStock() {
        return ResponseEntity.ok(vehicleModelService.getAllMddpStock());
    }
    @GetMapping("/finance")
    @Operation(summary = "Get finance details", description = "Returns list of finance records for Kendo Grid")
    public ResponseEntity<KendoGridResponse<FinanceDTO>> getFinanceDetails() {
        return ResponseEntity.ok(vehicleModelService.getAllFinanceDetails());
    }
    @GetMapping("/vehiclevariants")
    @Operation(summary = "Get all vehicle variants", description = "Returns a list of all vehicle variants for the Kendo Grid")
    public ResponseEntity<KendoGridResponse<VehicleVariant>> getAllVehicleVariants() {
        return ResponseEntity.ok(vehicleModelService.getAllVehicleVariants());
    }
    @GetMapping("/manufacturerOrders")
    @Operation(summary = "Get all manufacturer orders", description = "Returns manufacturer order data compatible with Kendo Grid")
    public ResponseEntity<KendoGridResponse<ManufacturerOrderDTO>> getAllManufacturerOrders() {
        return ResponseEntity.ok(vehicleModelService.getAllManufacturerOrders());
    }

    @PutMapping("/vehiclevariants/update")
    @Operation(summary = "Update vehicle variant(s)", description = "Updates one or multiple vehicle variants by VIN. Examples show field data types.")
    @RequestBody(
            description = "Vehicle variant(s) to update. Includes fields like vinNumber (string, required), vehicleModelId (integer), variant (string), price (number), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { VehicleVariantDTO.class, VehicleVariantDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Variant Update Example",
                                    summary = "Data type example for a single vehicle variant update",
                                    value = "{\"vehicleModelId\": \"integer\",\"modelName\":\"string\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Variants Update Example",
                                    summary = "Data type example for multiple vehicle variant updates",
                                    value = "[{\"vehicleModelId\": \"integer\",\"modelName\":\"string\",\"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<VehicleVariant>> updateVehicleVariants(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to update vehicle variants at {}", java.time.LocalDateTime.now());
        try {
            List<VehicleVariantDTO> dtos = normalizeToList(request, VehicleVariantDTO.class);
            log.info("Processing {} vehicle variant updates", dtos.size());
            KendoGridResponse<VehicleVariant> serviceResponse = vehicleModelService.updateVehicleVariants(dtos);
            List<VehicleVariant> updatedVariants = serviceResponse.getData();
            log.info("Successfully updated {} vehicle variants", updatedVariants.size());
            return ResponseEntity.ok(new KendoGridResponse<VehicleVariant>(updatedVariants, (long) updatedVariants.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid vehicle variant update request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<VehicleVariant>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating vehicle variants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<VehicleVariant>(Collections.emptyList(), 0L, "Error updating vehicle variants: " + e.getMessage(), null));
        }
    }
    @PutMapping("/manufacturerorders/update")
    @Operation(summary = "Update manufacturer order(s)", description = "Updates one or multiple manufacturer orders by VIN. Examples show field data types.")
    @RequestBody(
            description = "Manufacturer order(s) to update. Includes fields like manufacturerId (integer), vehicleVariantId (integer), manufacturerLocation (string), orderStatus (string), vinNumber (string, required), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { ManufacturerOrderDTO.class, ManufacturerOrderDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Manufacturer Order Update Example",
                                    summary = "Data type example for a single manufacturer order update",
                                    value = "{\"manufacturerId\": 2, \"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"2025-12-31T00:00:00\", \"modelName\": \"string\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Manufacturer Orders Update Example",
                                    summary = "Data type example for multiple manufacturer orders update",
                                    value = "[{\"manufacturerId\": 2, \"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"2025-12-31T00:00:00\", \"modelName\": \"string\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"updatedBy\": \"string\"}, {\"manufacturerId\": \"integer\", \"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"2025-12-31T00:00:00\", \"modelName\": \"string\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manufacturer orders updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<ManufacturerOrder>> updateManufacturerOrders(@Valid @org.springframework.web.bind.annotation.RequestBody List<ManufacturerOrderDTO> dtos) {
        log.info("Received request to update {} manufacturer orders at {}", dtos.size(), java.time.LocalDateTime.now());
        try {
            if (dtos == null || dtos.isEmpty()) {
                log.error("Request body is null or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Request body cannot be null or empty", null));
            }
            KendoGridResponse<ManufacturerOrder> serviceResponse = vehicleModelService.updateManufacturerOrders(dtos);
            List<ManufacturerOrder> updatedOrders = serviceResponse.getData();
            log.info("Successfully updated {} manufacturer orders", updatedOrders.size());
            return ResponseEntity.ok(new KendoGridResponse<>(updatedOrders, (long) updatedOrders.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid manufacturer order update request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating manufacturer orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error updating manufacturer orders: " + e.getMessage(), null));
        }
    }

    }

