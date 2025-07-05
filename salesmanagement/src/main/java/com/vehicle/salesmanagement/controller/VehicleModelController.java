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
    public ResponseEntity<KendoGridResponse<VehicleAttributesResponse>> getDropdownData(
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
                        .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Model name cannot be empty", null));
            }
            if (variant != null && variant.trim().isEmpty()) {
                log.error("Variant cannot be empty when provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Variant cannot be empty", null));
            }
            if (vehicleModelId != null && vehicleModelId <= 0) {
                log.error("Vehicle model ID must be positive when provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Vehicle model ID must be positive", null));
            }
            if (vehicleVariantId != null && vehicleVariantId <= 0) {
                log.error("Vehicle variant ID must be positive when provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Vehicle variant ID must be positive", null));
            }

            // Fetch dropdown data with IDs
            VehicleAttributesResponse response = vehicleModelService.getDropdownData(modelName, variant, vehicleModelId, vehicleVariantId);
            String message = buildResponseMessage(modelName, variant, vehicleModelId, vehicleVariantId);
            return ResponseEntity.ok(new KendoGridResponse<>(Collections.singletonList(response), 1L, message, null));
        } catch (NumberFormatException e) {
            log.error("Invalid numeric data provided: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid numeric data: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving dropdown data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Internal server error: " + e.getMessage(), null));
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
            List<VehicleModel> savedModels = vehicleModelService.saveVehicleModels(dtos).getData();
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
                                    value = "{\"vehicleModelId\": \"integer\", \"modelName\":\"string\",\"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Variants Example",
                                    summary = "Data type example for multiple vehicle variants",
                                    value = "[{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
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
            List<VehicleVariant> savedVariants = serviceResponse.getData();
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
            description = "Stock detail(s) to save. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { StockDetailsDTO.class, StockDetailsDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Stock Example",
                                    summary = "Data type example for a single stock detail",
                                    value = "{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Stock Example",
                                    summary = "Data type example for multiple stock details",
                                    value = "[{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<StockDetails>> saveStockDetails(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to save stock details at {}", java.time.LocalDateTime.now());
        try {
            List<StockDetailsDTO> dtos = normalizeToList(request, StockDetailsDTO.class);
            log.info("Processing {} stock details", dtos.size());
            KendoGridResponse<StockDetails> serviceResponse = vehicleModelService.saveStockDetails(dtos);
            List<StockDetails> savedStockDetails = serviceResponse.getData();
            log.info("Successfully saved {} stock details", savedStockDetails.size());
            return ResponseEntity.ok(new KendoGridResponse<>(savedStockDetails, (long) savedStockDetails.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error saving stock details: " + e.getMessage(), null));
        }
    }

    @PostMapping("/mddpstock/save")
    @Operation(summary = "Save MDDP stock detail(s)", description = "Saves one or multiple MDDP stock details. Examples show field data types.")
    @RequestBody(
            description = "MDDP stock detail(s) to save. Includes fields like vehicleModelId (integer), vehicleVariantId (integer), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { MddpStockDTO.class, MddpStockDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single MDDP Stock Example",
                                    summary = "Data type example for a single MDDP stock detail",
                                    value = "{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple MDDP Stock Example",
                                    summary = "Data type example for multiple MDDP stock details",
                                    value = "[{\"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<MddpStock>> saveMddpStock(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to save MDDP stock details at {}", java.time.LocalDateTime.now());
        try {
            List<MddpStockDTO> dtos = normalizeToList(request, MddpStockDTO.class);
            log.info("Processing {} MDDP stock details", dtos.size());
            KendoGridResponse<MddpStock> serviceResponse = vehicleModelService.saveMddpStock(dtos);
            List<MddpStock> savedMddpStock = serviceResponse.getData();
            log.info("Successfully saved {} MDDP stock details", savedMddpStock.size());
            return ResponseEntity.ok(new KendoGridResponse<>(savedMddpStock, (long) savedMddpStock.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving MDDP stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error saving MDDP stock details: " + e.getMessage(), null));
        }
    }

    @PostMapping("/vehiclevariants/update")
    @Operation(summary = "Update vehicle variant(s)", description = "Updates one or multiple vehicle variants. Examples show field data types.")
    @RequestBody(
            description = "Vehicle variant(s) to update. Includes fields like vehicleVariantId (integer), vehicleModelId (integer), variant (string), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { VehicleVariantDTO.class, VehicleVariantDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Variant Update Example",
                                    summary = "Data type example for updating a single vehicle variant",
                                    value = "{\"vehicleVariantId\": \"integer\", \"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Variants Update Example",
                                    summary = "Data type example for updating multiple vehicle variants",
                                    value = "[{\"vehicleVariantId\": \"integer\", \"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"updatedBy\": \"string\"}, {\"vehicleVariantId\": \"integer\", \"vehicleModelId\": \"integer\", \"variant\": \"string\", \"suffix\": \"string\", \"safetyFeature\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"interiorColour\": \"string\", \"engineCapacity\": \"string\", \"fuelType\": \"string\", \"price\": \"number\", \"yearOfManufacture\": \"integer\", \"bodyType\": \"string\", \"fuelTankCapacity\": \"number\", \"seatingCapacity\": \"integer\", \"maxPower\": \"string\", \"maxTorque\": \"string\", \"topSpeed\": \"string\", \"wheelBase\": \"string\", \"width\": \"string\", \"length\": \"string\", \"infotainment\": \"string\", \"comfort\": \"string\", \"numberOfAirBags\": \"integer\", \"mileageCity\": \"number\", \"mileageHighway\": \"number\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<VehicleVariant>> updateVehicleVariants(@org.springframework.web.bind.annotation.RequestBody Object request) {
        log.info("Received request to update vehicle variants at {}", java.time.LocalDateTime.now());
        try {
            List<VehicleVariantDTO> dtos = normalizeToList(request, VehicleVariantDTO.class);
            log.info("Processing {} vehicle variants for update", dtos.size());
            KendoGridResponse<VehicleVariant> serviceResponse = vehicleModelService.updateVehicleVariants(dtos);
            List<VehicleVariant> updatedVariants = serviceResponse.getData();
            log.info("Successfully updated {} vehicle variants", updatedVariants.size());
            return ResponseEntity.ok(new KendoGridResponse<>(updatedVariants, (long) updatedVariants.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating vehicle variants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error updating vehicle variants: " + e.getMessage(), null));
        }
    }

    @PostMapping("/stockdetails/update")
    @Operation(summary = "Update stock detail(s)", description = "Updates one or multiple stock details. Examples show field data types.")
    @RequestBody(
            description = "Stock detail(s) to update. Includes fields like stockId (integer), vehicleModelId (integer), vehicleVariantId (integer), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { StockDetailsDTO.class, StockDetailsDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Stock Update Example",
                                    summary = "Data type example for updating a single stock detail",
                                    value = "{\"stockId\": \"integer\", \"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Stock Update Example",
                                    summary = "Data type example for updating multiple stock details",
                                    value = "[{\"stockId\": \"integer\", \"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"updatedBy\": \"string\"}, {\"stockId\": \"integer\", \"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<StockDetails>> updateStockDetails(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to update stock details at {}", java.time.LocalDateTime.now());
        try {
            List<StockDetailsDTO> dtos = normalizeToList(request, StockDetailsDTO.class);
            log.info("Processing {} stock details for update", dtos.size());
            KendoGridResponse<StockDetails> serviceResponse = vehicleModelService.updateStockDetails(dtos);
            List<StockDetails> updatedStockDetails = serviceResponse.getData();
            log.info("Successfully updated {} stock details", updatedStockDetails.size());
            return ResponseEntity.ok(new KendoGridResponse<>(updatedStockDetails, (long) updatedStockDetails.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error updating stock details: " + e.getMessage(), null));
        }
    }

    @PostMapping("/mddpstock/update")
    @Operation(summary = "Update MDDP stock detail(s)", description = "Updates one or multiple MDDP stock details. Examples show field data types.")
    @RequestBody(
            description = "MDDP stock detail(s) to update. Includes fields like mddpId (integer), vehicleModelId (integer), vehicleVariantId (integer), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { MddpStockDTO.class, MddpStockDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single MDDP Stock Update Example",
                                    summary = "Data type example for updating a single MDDP stock detail",
                                    value = "{\"mddpId\": \"integer\", \"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple MDDP Stock Update Example",
                                    summary = "Data type example for updating multiple MDDP stock details",
                                    value = "[{\"mddpId\": \"integer\", \"vehicleModelId\": \"integer\", \"modelName\":\"string\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\", \"updatedBy\": \"string\"}, {\"mddpId\": \"integer\", \"vehicleModelId\": \"integer\", \"vehicleVariantId\": \"integer\", \"suffix\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"variant\": \"string\", \"interiorColour\": \"string\", \"quantity\": \"integer\", \"stockStatus\": \"string\", \"expectedDispatchDate\": \"string\", \"expectedDeliveryDate\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<MddpStock>> updateMddpStock(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to update MDDP stock details at {}", java.time.LocalDateTime.now());
        try {
            List<MddpStockDTO> dtos = normalizeToList(request, MddpStockDTO.class);
            log.info("Processing {} MDDP stock details for update", dtos.size());
            KendoGridResponse<MddpStock> serviceResponse = vehicleModelService.updateMddpStock(dtos);
            List<MddpStock> updatedMddpStock = serviceResponse.getData();
            log.info("Successfully updated {} MDDP stock details", updatedMddpStock.size());
            return ResponseEntity.ok(new KendoGridResponse<>(updatedMddpStock, (long) updatedMddpStock.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating MDDP stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error updating MDDP stock details: " + e.getMessage(), null));
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
                                    name = "Single Manufacturer Order Example",
                                    summary = "Data type example for a single manufacturer order",
                                    value = "{\"vehicleVariantId\": \"integer\", \"modelName\": \"string\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"variant\": \"string\", \"suffix\": \"string\", \"interiorColour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Manufacturer Orders Example",
                                    summary = "Data type example for multiple manufacturer orders",
                                    value = "[{\"vehicleVariantId\": \"integer\", \"modelName\": \"string\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"variant\": \"string\", \"suffix\": \"string\", \"interiorColour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}, {\"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"variant\": \"string\", \"suffix\": \"string\", \"interiorColour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"createdBy\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<ManufacturerOrder>> saveManufacturerOrders(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to save manufacturer orders at {}", java.time.LocalDateTime.now());
        try {
            List<ManufacturerOrderDTO> dtos = normalizeToList(request, ManufacturerOrderDTO.class);
            log.info("Processing {} manufacturer orders", dtos.size());
            KendoGridResponse<ManufacturerOrder> serviceResponse = vehicleModelService.saveManufacturerOrders(dtos);
            List<ManufacturerOrder> savedOrders = serviceResponse.getData();
            log.info("Successfully saved {} manufacturer orders", savedOrders.size());
            return ResponseEntity.ok(new KendoGridResponse<>(savedOrders, (long) savedOrders.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving manufacturer orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error saving manufacturer orders: " + e.getMessage(), null));
        }
    }

    @PostMapping("/manufacturerorders/update")
    @Operation(summary = "Update manufacturer order(s)", description = "Updates one or multiple manufacturer orders. Examples show field data types.")
    @RequestBody(
            description = "Manufacturer order(s) to update. Includes fields like manufacturerId (integer), vehicleVariantId (integer), etc. Examples show data types.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(anyOf = { ManufacturerOrderDTO.class, ManufacturerOrderDTO[].class }),
                    examples = {
                            @ExampleObject(
                                    name = "Single Manufacturer Order Update Example",
                                    summary = "Data type example for updating a single manufacturer order",
                                    value = "{\"manufacturerId\": \"integer\", \"vehicleVariantId\": \"integer\", \"modelName\": \"string\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"variant\": \"string\", \"suffix\": \"string\", \"interiorColour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"updatedBy\": \"string\"}"
                            ),
                            @ExampleObject(
                                    name = "Multiple Manufacturer Orders Update Example",
                                    summary = "Data type example for updating multiple manufacturer orders",
                                    value = "[{\"manufacturerId\": \"integer\", \"vehicleVariantId\": \"integer\", \"modelName\": \"string\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"variant\": \"string\", \"suffix\": \"string\", \"interiorColour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"updatedBy\": \"string\"}, {\"manufacturerId\": \"integer\", \"vehicleVariantId\": \"integer\", \"manufacturerLocation\": \"string\", \"orderStatus\": \"string\", \"estimatedArrivalDate\": \"string\", \"fuelType\": \"string\", \"colour\": \"string\", \"variant\": \"string\", \"suffix\": \"string\", \"interiorColour\": \"string\", \"engineColour\": \"string\", \"transmissionType\": \"string\", \"updatedBy\": \"string\"}]"
                            )
                    }
            )
    )
    public ResponseEntity<KendoGridResponse<ManufacturerOrder>> updateManufacturerOrders(@org.springframework.web.bind.annotation.RequestBody Object request) throws JsonProcessingException {
        log.info("Received request to update manufacturer orders at {}", java.time.LocalDateTime.now());
        try {
            List<ManufacturerOrderDTO> dtos = normalizeToList(request, ManufacturerOrderDTO.class);
            log.info("Processing {} manufacturer orders for update", dtos.size());
            KendoGridResponse<ManufacturerOrder> serviceResponse = vehicleModelService.updateManufacturerOrders(dtos);
            List<ManufacturerOrder> updatedOrders = serviceResponse.getData();
            log.info("Successfully updated {} manufacturer orders", updatedOrders.size());
            return ResponseEntity.ok(new KendoGridResponse<>(updatedOrders, (long) updatedOrders.size(), null, null));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Invalid request: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error updating manufacturer orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error updating manufacturer orders: " + e.getMessage(), null));
        }
    }

    @GetMapping("/stockdetails")
    @Operation(summary = "Get all stock details", description = "Retrieves all stock details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock details retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<StockDetailsDTO>> getAllStockDetails() {
        log.info("Received request to fetch all stock details at {}", java.time.LocalDateTime.now());
        try {
            List<StockDetailsDTO> stockDetails = vehicleModelService.getAllStockDetails();
            log.info("Successfully retrieved {} stock details", stockDetails.size());
            return ResponseEntity.ok(new KendoGridResponse<>(stockDetails, (long) stockDetails.size(), null, null));
        } catch (Exception e) {
            log.error("Error retrieving stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error retrieving stock details: " + e.getMessage(), null));
        }
    }

    @GetMapping("/mddpstock")
    @Operation(summary = "Get all MDDP stock details", description = "Retrieves all MDDP stock details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MDDP stock details retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<MddpStockDTO>> getAllMddpStock() {
        log.info("Received request to fetch all MDDP stock details at {}", java.time.LocalDateTime.now());
        try {
            KendoGridResponse<MddpStockDTO> serviceResponse = vehicleModelService.getAllMddpStock();
            List<MddpStockDTO> mddpStock = serviceResponse.getData();
            log.info("Successfully retrieved {} MDDP stock details", mddpStock.size());
            return ResponseEntity.ok(new KendoGridResponse<>(mddpStock, (long) mddpStock.size(), null, null));
        } catch (Exception e) {
            log.error("Error retrieving MDDP stock details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error retrieving MDDP stock details: " + e.getMessage(), null));
        }
    }

    @GetMapping("/financedetails")
    @Operation(summary = "Get all finance details", description = "Retrieves all finance details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Finance details retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<FinanceDTO>> getAllFinanceDetails() {
        log.info("Received request to fetch all finance details at {}", java.time.LocalDateTime.now());
        try {
            KendoGridResponse<FinanceDTO> serviceResponse = vehicleModelService.getAllFinanceDetails();
            List<FinanceDTO> financeDetails = serviceResponse.getData();
            log.info("Successfully retrieved {} finance details", financeDetails.size());
            return ResponseEntity.ok(new KendoGridResponse<>(financeDetails, (long) financeDetails.size(), null, null));
        } catch (Exception e) {
            log.error("Error retrieving finance details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error retrieving finance details: " + e.getMessage(), null));
        }
    }

    @GetMapping("/manufacturerorders")
    @Operation(summary = "Get all manufacturer orders", description = "Retrieves all manufacturer orders")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manufacturer orders retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<ManufacturerOrderDTO>> getAllManufacturerOrders() {
        log.info("Received request to fetch all manufacturer orders at {}", java.time.LocalDateTime.now());
        try {
            KendoGridResponse<ManufacturerOrderDTO> serviceResponse = vehicleModelService.getAllManufacturerOrders();
            List<ManufacturerOrderDTO> orders = serviceResponse.getData();
            log.info("Successfully retrieved {} manufacturer orders", orders.size());
            return ResponseEntity.ok(new KendoGridResponse<>(orders, (long) orders.size(), null, null));
        } catch (Exception e) {
            log.error("Error retrieving manufacturer orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error retrieving manufacturer orders: " + e.getMessage(), null));
        }
    }

    @GetMapping("/vehiclevariants")
    @Operation(summary = "Get all vehicle variants", description = "Retrieves all vehicle variants")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle variants retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<VehicleVariant>> getAllVehicleVariants() {
        log.info("Received request to fetch all vehicle variants at {}", java.time.LocalDateTime.now());
        try {
            KendoGridResponse<VehicleVariant> serviceResponse = vehicleModelService.getAllVehicleVariants();
            List<VehicleVariant> variants = serviceResponse.getData();
            log.info("Successfully retrieved {} vehicle variants", variants.size());
            return ResponseEntity.ok(new KendoGridResponse<>(variants, (long) variants.size(), null, null));
        } catch (Exception e) {
            log.error("Error retrieving vehicle variants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error retrieving vehicle variants: " + e.getMessage(), null));
        }
    }

    @GetMapping("/stockdetails/find")
    @Operation(summary = "Get stock detail by model and variant", description = "Retrieves stock detail for a specific model name and vehicle variant ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Stock detail retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = StockDetailsDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid model name or variant ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Stock detail not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<StockDetailsDTO>> getStockDetailByModelAndVariant(
            @RequestParam(value = "modelName") String modelName,
            @RequestParam(value = "vehicleVariantId") Long vehicleVariantId) {
        log.info("Received request to fetch stock detail for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
        try {
            StockDetailsDTO stockDetail = vehicleModelService.getStockDetailByModelAndVariant(modelName, vehicleVariantId);
            KendoGridResponse<StockDetailsDTO> response = new KendoGridResponse<>();
            if (stockDetail == null) {
                log.warn("No stock detail found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            log.info("Successfully retrieved stock detail for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
            response.setData(Collections.singletonList(stockDetail));
            response.setTotal(1);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            KendoGridResponse<StockDetailsDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error retrieving stock detail: {}", e.getMessage(), e);
            KendoGridResponse<StockDetailsDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/mddpstock/find")
    @Operation(summary = "Get MDDP stock detail by model and variant", description = "Retrieves MDDP stock detail for a specific model name and vehicle variant ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MDDP stock detail retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MddpStockDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid model name or variant ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MDDP stock detail not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<MddpStockDTO>> getMddpStockByModelAndVariant(
            @RequestParam(value = "modelName") String modelName,
            @RequestParam(value = "vehicleVariantId") Long vehicleVariantId) {
        log.info("Received request to fetch MDDP stock detail for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
        try {
            MddpStockDTO mddpStock = vehicleModelService.getMddpStockByModelAndVariant(modelName, vehicleVariantId);
            KendoGridResponse<MddpStockDTO> response = new KendoGridResponse<>();
            if (mddpStock == null) {
                log.warn("No MDDP stock detail found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            log.info("Successfully retrieved MDDP stock detail for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
            response.setData(Collections.singletonList(mddpStock));
            response.setTotal(1);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            KendoGridResponse<MddpStockDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error retrieving MDDP stock detail: {}", e.getMessage(), e);
            KendoGridResponse<MddpStockDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/manufacturerorders/find")
    @Operation(summary = "Get manufacturer order by model and variant", description = "Retrieves manufacturer order for a specific model name and vehicle variant ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manufacturer order retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ManufacturerOrderDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid model name or variant ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Manufacturer order not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<ManufacturerOrderDTO>> getManufacturerOrderByModelAndVariant(
            @RequestParam(value = "modelName") String modelName,
            @RequestParam(value = "vehicleVariantId") Long vehicleVariantId) {
        log.info("Received request to fetch manufacturer order for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
        try {
            ManufacturerOrderDTO order = vehicleModelService.getManufacturerOrderByModelAndVariant(modelName, vehicleVariantId);
            KendoGridResponse<ManufacturerOrderDTO> response = new KendoGridResponse<>();
            if (order == null) {
                log.warn("No manufacturer order found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            log.info("Successfully retrieved manufacturer order for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
            response.setData(Collections.singletonList(order));
            response.setTotal(1);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            KendoGridResponse<ManufacturerOrderDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error retrieving manufacturer order: {}", e.getMessage(), e);
            KendoGridResponse<ManufacturerOrderDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/vehiclevariants/find")
    @Operation(summary = "Get vehicle variant by model and variant", description = "Retrieves vehicle variant for a specific model name and vehicle variant ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vehicle variant retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VehicleVariantDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid model name or variant ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vehicle variant not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KendoGridResponse.class)))
    })
    public ResponseEntity<KendoGridResponse<VehicleVariantDTO>> getVehicleVariantByModelAndVariant(
            @RequestParam(value = "modelName") String modelName,
            @RequestParam(value = "vehicleVariantId") Long vehicleVariantId) {
        log.info("Received request to fetch vehicle variant for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
        try {
            VehicleVariantDTO variant = vehicleModelService.getVehicleVariantByModelAndVariant(modelName, vehicleVariantId);
            KendoGridResponse<VehicleVariantDTO> response = new KendoGridResponse<>();
            if (variant == null) {
                log.warn("No vehicle variant found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            log.info("Successfully retrieved vehicle variant for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
            response.setData(Collections.singletonList(variant));
            response.setTotal(1);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            KendoGridResponse<VehicleVariantDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error retrieving vehicle variant: {}", e.getMessage(), e);
            KendoGridResponse<VehicleVariantDTO> response = new KendoGridResponse<>();
            response.setData(Collections.emptyList());
            response.setTotal(0);
            response.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}