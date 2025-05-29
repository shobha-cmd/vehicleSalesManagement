package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
                                    value = "{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Stock Example",
                                    summary = "Data type example for multiple stock details",
                                    value = "[{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
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
                                    value = "{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple MDDP Stock Example",
                                    summary = "Data type example for multiple MDDP stock details",
                                    value = "[{\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"vinNumber\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"2025-12-31T00:00:00\", \"expectedDeliveryDate\": \"2026-01-07T00:00:00\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
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
    public ResponseEntity<KendoGridResponse<ManufacturerOrder>> saveManufacturerOrders(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
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
}