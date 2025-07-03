package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoGridResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleAttributesResponse;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleModelService {

    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleVariantRepository vehicleVariantRepository;
    private final StockDetailsRepository stockDetailsRepository;
    private final MddpStockRepository mddpStockRepository;
    private final ManufacturerOrderRepository manufacturerOrderRepository;
    private final FinanceDetailsRepository financeDetailsRepository;

    public VehicleAttributesResponse getDropdownData(String modelName, String variant, Long vehicleModelId, Long vehicleVariantId) {
        log.info("Fetching dropdown data with filters: modelName={}, variant={}, vehicleModelId={}, vehicleVariantId={} at {}",
                modelName, variant, vehicleModelId, vehicleVariantId, LocalDateTime.now());

        VehicleAttributesResponse response = new VehicleAttributesResponse();

        // Step 1: Fetch models
        List<VehicleModel> models;
        if (vehicleModelId != null) {
            models = vehicleModelRepository.findById(vehicleModelId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (modelName != null) {
            models = vehicleModelRepository.findByModelNameIgnoreCase(modelName);
        } else {
            models = vehicleModelRepository.findAll();
        }

        // Set model names for the first dropdown
        List<String> modelNames = models.stream()
                .map(model -> model.getModelName() != null ? model.getModelName() : "")
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        response.setModelNames(modelNames);

        // If no model is selected, return only model names
        if (modelName == null && vehicleModelId == null) {
            log.info("No model selected, returning only model names");
            return response;
        }

        // Initialize model details map
        Map<String, VehicleAttributesResponse.ModelAttributes> modelDetails = new HashMap<>();
        response.setModelDetails(modelDetails);

        // If no models match the filter, return empty attributes
        if (models.isEmpty() && modelName != null) {
            VehicleAttributesResponse.ModelAttributes emptyAttributes = new VehicleAttributesResponse.ModelAttributes();
            modelDetails.put(modelName, emptyAttributes);
            log.info("No models found for modelName={}", modelName);
            return response;
        }

        // Step 2: Fetch variants based on selected model
        List<VehicleVariant> variants;
        if (vehicleVariantId != null) {
            variants = vehicleVariantRepository.findById(vehicleVariantId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (modelName != null && variant != null) {
            variants = vehicleVariantRepository.findByVehicleModelId_ModelNameAndVariant(modelName, variant);
        } else if (modelName != null) {
            variants = vehicleVariantRepository.findByVehicleModelId_ModelName(modelName);
        } else if (vehicleModelId != null) {
            variants = vehicleVariantRepository.findByVehicleModelId_VehicleModelId(vehicleModelId);
        } else {
            List<Long> modelIds = models.stream()
                    .map(VehicleModel::getVehicleModelId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            variants = vehicleVariantRepository.findByVehicleModelId_VehicleModelIdIn(modelIds);
        }

        // Filter variants by model IDs
        List<Long> modelIds = models.stream()
                .map(VehicleModel::getVehicleModelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        variants = variants.stream()
                .filter(v -> v.getVehicleModelId() != null && modelIds.contains(v.getVehicleModelId().getVehicleModelId()))
                .collect(Collectors.toList());

        // If no variants match, return empty attributes
        if (variants.isEmpty() && (modelName != null || vehicleModelId != null)) {
            String key = modelName != null ? modelName : models.get(0).getModelName();
            VehicleAttributesResponse.ModelAttributes emptyAttributes = new VehicleAttributesResponse.ModelAttributes();
            if (variant != null) {
                emptyAttributes.setVariants(Collections.singletonList(new VehicleAttributesResponse.Variant(variant, null)));
            }
            modelDetails.put(key, emptyAttributes);
            log.info("No variants found for modelName={} or vehicleModelId={}", modelName, vehicleModelId);
            return response;
        }

        // Step 3: Group variants by model name and populate attributes
        Map<String, List<VehicleVariant>> variantsByModel = variants.stream()
                .filter(v -> v.getVehicleModelId() != null && v.getVehicleModelId().getModelName() != null)
                .collect(Collectors.groupingBy(v -> v.getVehicleModelId().getModelName()));

        variantsByModel.forEach((mName, modelVariants) -> {
            VehicleAttributesResponse.ModelAttributes attributes = new VehicleAttributesResponse.ModelAttributes();

            // Set vehicleModelId
            Long modelId = modelVariants.stream()
                    .map(v -> v.getVehicleModelId().getVehicleModelId())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            attributes.setVehicleModelId(modelId);

            // Set variants for the second dropdown
            List<VehicleAttributesResponse.Variant> variantList = modelVariants.stream()
                    .map(v -> new VehicleAttributesResponse.Variant(v.getVariant(), v.getVehicleVariantId()))
                    .filter(v -> v.getName() != null)
                    .distinct()
                    .sorted(Comparator.comparing(VehicleAttributesResponse.Variant::getName))
                    .collect(Collectors.toList());
            attributes.setVariants(variantList);

            // Log variant count
            log.info("Processing model: {}, variants count: {}", mName, modelVariants.size());

            // Step 4: Filter variants by selected variant (if provided)
            List<VehicleVariant> filteredVariants = modelVariants;
            if (variant != null) {
                filteredVariants = modelVariants.stream()
                        .filter(v -> v.getVariant() != null && v.getVariant().equalsIgnoreCase(variant))
                        .collect(Collectors.toList());
            } else if (vehicleVariantId != null) {
                filteredVariants = modelVariants.stream()
                        .filter(v -> v.getVehicleVariantId() != null && v.getVehicleVariantId().equals(vehicleVariantId))
                        .collect(Collectors.toList());
            }
            log.info("Filtered variants for model {}: count={}", mName, filteredVariants.size());

            // Log sample data for debugging
            filteredVariants.forEach(v -> log.debug("Variant ID {}: interior_colour={}, fuel_type={}, price={}",
                    v.getVehicleVariantId(), v.getInteriorColour(), v.getFuelType(), v.getPrice()));

            // Populate attributes
            attributes.setColours(filteredVariants.stream()
                    .map(VehicleVariant::getColour)
                    .filter(Objects::nonNull)
                    .flatMap(colour -> Arrays.stream(colour.split(",\\s*")))
                    .filter(colour -> !colour.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Colours: {}", attributes.getColours());

            attributes.setEngineColours(filteredVariants.stream()
                    .map(VehicleVariant::getEngineColour)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Engine colours: {}", attributes.getEngineColours());

            attributes.setInteriorColours(filteredVariants.stream()
                    .map(VehicleVariant::getInteriorColour)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Interior colours: {}", attributes.getInteriorColours());

            attributes.setFuelTypes(filteredVariants.stream()
                    .map(VehicleVariant::getFuelType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Fuel types: {}", attributes.getFuelTypes());

            attributes.setTransmissionTypes(filteredVariants.stream()
                    .map(VehicleVariant::getTransmissionType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Transmission types: {}", attributes.getTransmissionTypes());

            attributes.setPrices(filteredVariants.stream()
                    .map(VehicleVariant::getPrice)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Collections.reverseOrder())
                    .collect(Collectors.toList()));
            log.debug("Prices: {}", attributes.getPrices());

            attributes.setYearsOfManufacture(filteredVariants.stream()
                    .map(VehicleVariant::getYearOfManufacture)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Years of manufacture: {}", attributes.getYearsOfManufacture());

            attributes.setBodyTypes(filteredVariants.stream()
                    .map(VehicleVariant::getBodyType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Body types: {}", attributes.getBodyTypes());

            attributes.setFuelTankCapacities(filteredVariants.stream()
                    .map(VehicleVariant::getFuelTankCapacity)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::doubleValue)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Fuel tank capacities: {}", attributes.getFuelTankCapacities());

            attributes.setNumberOfAirbags(filteredVariants.stream()
                    .map(VehicleVariant::getNumberOfAirBags)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Number of airbags: {}", attributes.getNumberOfAirbags());

            attributes.setMileageCities(filteredVariants.stream()
                    .map(VehicleVariant::getMileageCity)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::doubleValue)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Mileage cities: {}", attributes.getMileageCities());

            attributes.setMileageHighways(filteredVariants.stream()
                    .map(VehicleVariant::getMileageHighway)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::doubleValue)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Mileage highways: {}", attributes.getMileageHighways());

            attributes.setSeatingCapacities(filteredVariants.stream()
                    .map(VehicleVariant::getSeatingCapacity)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Seating capacities: {}", attributes.getSeatingCapacities());

            attributes.setMaxPowers(filteredVariants.stream()
                    .map(VehicleVariant::getMaxPower)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Max powers: {}", attributes.getMaxPowers());

            attributes.setMaxTorques(filteredVariants.stream()
                    .map(VehicleVariant::getMaxTorque)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Max torques: {}", attributes.getMaxTorques());

            attributes.setTopSpeeds(filteredVariants.stream()
                    .map(VehicleVariant::getTopSpeed)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Top speeds: {}", attributes.getTopSpeeds());

            attributes.setWheelBases(filteredVariants.stream()
                    .map(VehicleVariant::getWheelBase)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Wheel bases: {}", attributes.getWheelBases());

            attributes.setWidths(filteredVariants.stream()
                    .map(VehicleVariant::getWidth)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Widths: {}", attributes.getWidths());

            attributes.setLengths(filteredVariants.stream()
                    .map(VehicleVariant::getLength)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Lengths: {}", attributes.getLengths());

            attributes.setSafetyFeatures(filteredVariants.stream()
                    .map(VehicleVariant::getSafetyFeature)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Safety features: {}", attributes.getSafetyFeatures());

            attributes.setInfotainments(filteredVariants.stream()
                    .map(VehicleVariant::getInfotainment)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Infotainments: {}", attributes.getInfotainments());

            attributes.setComforts(filteredVariants.stream()
                    .map(VehicleVariant::getComfort)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Comforts: {}", attributes.getComforts());

            attributes.setSuffixes(filteredVariants.stream()
                    .map(VehicleVariant::getSuffix)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Suffixes: {}", attributes.getSuffixes());

            attributes.setEngineCapacities(filteredVariants.stream()
                    .map(VehicleVariant::getEngineCapacity)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));
            log.debug("Engine capacities: {}", attributes.getEngineCapacities());

            modelDetails.put(mName, attributes);
        });

        log.info("Successfully fetched dropdown data for modelName={}", modelName);
        return response;
    }

    @Transactional
    public KendoGridResponse<VehicleModel> saveVehicleModels(List<VehicleModelDTO> dtos) {
        log.info("Starting insertion of {} vehicle models at {}", dtos != null ? dtos.size() : 0, LocalDateTime.now());

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received null or empty DTO list");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Vehicle model list cannot be null or empty", null);
        }

        List<VehicleModel> vehicleModelsToSave = dtos.stream()
                .peek(dto -> {
                    if (dto.getModelName() == null || dto.getModelName().trim().isEmpty()) {
                        log.error("Invalid modelName in DTO: {}", dto);
                        throw new IllegalArgumentException("modelName cannot be null or empty");
                    }
                })
                .filter(dto -> {
                    boolean exists = vehicleModelRepository.findByModelName(dto.getModelName()).isPresent();
                    if (exists) {
                        log.warn("Skipping duplicate model: {}", dto.getModelName());
                        return false;
                    }
                    return true;
                })
                .map(dto -> {
                    VehicleModel model = new VehicleModel();
                    model.setModelName(dto.getModelName());
                    return model;
                })
                .collect(Collectors.toList());

        try {
            List<VehicleModel> savedModels = vehicleModelRepository.saveAll(vehicleModelsToSave);
            log.info("Successfully saved {} vehicle models", savedModels.size());
            return new KendoGridResponse<>(savedModels, (long) savedModels.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to save vehicle models: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to save vehicle models: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<VehicleVariant> saveVehicleVariants(List<VehicleVariantDTO> dtos) {
        log.info("Saving {} vehicle variants", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Vehicle variant list is empty");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Vehicle variant list cannot be empty", null);
        }

        List<VehicleVariant> variantsToSave = dtos.stream()
                .peek(dto -> {
                    if (dto.getVariant() == null || dto.getVariant().trim().isEmpty()) {
                        log.error("Variant name is required");
                        throw new IllegalArgumentException("Variant name is required");
                    }
                    if (dto.getVehicleModelId() == null) {
                        log.error("VehicleModel ID is required");
                        throw new IllegalArgumentException("VehicleModel ID is required");
                    }
                    if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                        log.error("VIN number is required");
                        throw new IllegalArgumentException("VIN number is required");
                    }
                })
                .filter(dto -> {
                    Optional<VehicleVariant> existing = vehicleVariantRepository.findByVinNumber(dto.getVinNumber());
                    if (existing.isPresent()) {
                        log.warn("Duplicate VIN {} skipped", dto.getVinNumber());
                        return false;
                    }
                    return true;
                })
                .map(dto -> {
                    VehicleVariant variant = new VehicleVariant();
                    variant.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
                    variant.setVariant(dto.getVariant());
                    variant.setModelName(dto.getModelName());
                    variant.setSuffix(dto.getSuffix());
                    variant.setSafetyFeature(dto.getSafetyFeature());
                    variant.setColour(dto.getColour());
                    variant.setEngineColour(dto.getEngineColour());
                    variant.setTransmissionType(dto.getTransmissionType());
                    variant.setInteriorColour(dto.getInteriorColour());
                    variant.setVinNumber(dto.getVinNumber());
                    variant.setEngineCapacity(dto.getEngineCapacity());
                    variant.setFuelType(dto.getFuelType());
                    variant.setPrice(dto.getPrice());
                    variant.setYearOfManufacture(dto.getYearOfManufacture());
                    variant.setBodyType(dto.getBodyType());
                    variant.setFuelTankCapacity(dto.getFuelTankCapacity());
                    variant.setSeatingCapacity(dto.getSeatingCapacity());
                    variant.setMaxPower(dto.getMaxPower());
                    variant.setMaxTorque(dto.getMaxTorque());
                    variant.setTopSpeed(dto.getTopSpeed());
                    variant.setWheelBase(dto.getWheelBase());
                    variant.setWidth(dto.getWidth());
                    variant.setLength(dto.getLength());
                    variant.setInfotainment(dto.getInfotainment());
                    variant.setComfort(dto.getComfort());
                    variant.setNumberOfAirBags(dto.getNumberOfAirBags());
                    variant.setMileageCity(dto.getMileageCity());
                    variant.setMileageHighway(dto.getMileageHighway());
                    return variant;
                })
                .collect(Collectors.toList());

        try {
            List<VehicleVariant> savedVariants = vehicleVariantRepository.saveAll(variantsToSave);
            log.info("Successfully saved {} vehicle variants", savedVariants.size());
            return new KendoGridResponse<>(savedVariants, (long) savedVariants.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to save vehicle variants: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to save vehicle variants: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<VehicleVariant> updateVehicleVariants(List<VehicleVariantDTO> dtos) {
        log.info("Updating {} vehicle variant entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty vehicle variant list for update");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Vehicle variant list cannot be empty", null);
        }

        List<VehicleVariant> updatedVehicleVariants = new ArrayList<>();

        for (VehicleVariantDTO dto : dtos) {
            if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                log.error("VIN number is required for update");
                return new KendoGridResponse<>(Collections.emptyList(), 0L, "VIN number is required", null);
            }

            VehicleVariant existingVariant = vehicleVariantRepository.findByVinNumber(dto.getVinNumber())
                    .orElseThrow(() -> {
                        log.error("VehicleVariant not found with VIN: {}", dto.getVinNumber());
                        return new IllegalArgumentException("VehicleVariant not found with VIN: " + dto.getVinNumber());
                    });

            if (dto.getVehicleModelId() != null) {
                existingVariant.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
            }
            if (dto.getModelName() != null) {
                existingVariant.setModelName(dto.getModelName());
            }
            if (dto.getVariant() != null) {
                existingVariant.setVariant(dto.getVariant());
            }
            if (dto.getSuffix() != null) {
                existingVariant.setSuffix(dto.getSuffix());
            }
            if (dto.getSafetyFeature() != null) {
                existingVariant.setSafetyFeature(dto.getSafetyFeature());
            }
            if (dto.getColour() != null) {
                existingVariant.setColour(dto.getColour());
            }
            if (dto.getEngineColour() != null) {
                existingVariant.setEngineColour(dto.getEngineColour());
            }
            if (dto.getTransmissionType() != null) {
                existingVariant.setTransmissionType(dto.getTransmissionType());
            }
            if (dto.getInteriorColour() != null) {
                existingVariant.setInteriorColour(dto.getInteriorColour());
            }
            if (dto.getEngineCapacity() != null) {
                existingVariant.setEngineCapacity(dto.getEngineCapacity());
            }
            if (dto.getFuelType() != null) {
                existingVariant.setFuelType(dto.getFuelType());
            }
            if (dto.getPrice() != null) {
                existingVariant.setPrice(dto.getPrice());
            }
            if (dto.getYearOfManufacture() != null) {
                existingVariant.setYearOfManufacture(dto.getYearOfManufacture());
            }
            if (dto.getBodyType() != null) {
                existingVariant.setBodyType(dto.getBodyType());
            }
            if (dto.getFuelTankCapacity() != null) {
                existingVariant.setFuelTankCapacity(dto.getFuelTankCapacity());
            }
            if (dto.getSeatingCapacity() != null) {
                existingVariant.setSeatingCapacity(dto.getSeatingCapacity());
            }
            if (dto.getMaxPower() != null) {
                existingVariant.setMaxPower(dto.getMaxPower());
            }
            if (dto.getMaxTorque() != null) {
                existingVariant.setMaxTorque(dto.getMaxTorque());
            }
            if (dto.getTopSpeed() != null) {
                existingVariant.setTopSpeed(dto.getTopSpeed());
            }
            if (dto.getWheelBase() != null) {
                existingVariant.setWheelBase(dto.getWheelBase());
            }
            if (dto.getWidth() != null) {
                existingVariant.setWidth(dto.getWidth());
            }
            if (dto.getLength() != null) {
                existingVariant.setLength(dto.getLength());
            }
            if (dto.getInfotainment() != null) {
                existingVariant.setInfotainment(dto.getInfotainment());
            }
            if (dto.getComfort() != null) {
                existingVariant.setComfort(dto.getComfort());
            }
            if (dto.getNumberOfAirBags() != null) {
                existingVariant.setNumberOfAirBags(dto.getNumberOfAirBags());
            }
            if (dto.getMileageCity() != null) {
                existingVariant.setMileageCity(dto.getMileageCity());
            }
            if (dto.getMileageHighway() != null) {
                existingVariant.setMileageHighway(dto.getMileageHighway());
            }

            updatedVehicleVariants.add(existingVariant);
        }

        try {
            List<VehicleVariant> savedVehicleVariants = vehicleVariantRepository.saveAll(updatedVehicleVariants);
            log.info("Successfully updated {} vehicle variants entries", savedVehicleVariants.size());
            return new KendoGridResponse<>(savedVehicleVariants, (long) savedVehicleVariants.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to update vehicle variants: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to update vehicle variants: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<StockDetails> saveStockDetails(List<StockDetailsDTO> dtos) {
        log.info("Saving {} stock entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty stock details list");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Stock details list cannot be empty", null);
        }

        List<StockDetails> stockDetailsToSave = dtos.stream()
                .peek(dto -> {
                    if (dto.getVehicleModelId() == null) {
                        log.error("VehicleModel ID is required");
                        throw new IllegalArgumentException("VehicleModel ID is required");
                    }
                    if (dto.getVehicleVariantId() == null) {
                        log.error("VehicleVariant ID is required");
                        throw new IllegalArgumentException("VehicleVariant ID is required");
                    }
                    if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                        log.error("VIN number is required");
                        throw new IllegalArgumentException("VIN number is required");
                    }
                })
                .map(dto -> {
                    StockDetails stock = new StockDetails();
                    stock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
                    stock.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
                    stock.setSuffix(dto.getSuffix());
                    stock.setModelName(dto.getModelName());
                    stock.setFuelType(dto.getFuelType());
                    stock.setColour(dto.getColour());
                    stock.setEngineColour(dto.getEngineColour());
                    stock.setTransmissionType(dto.getTransmissionType());
                    stock.setVariant(dto.getVariant());
                    stock.setQuantity(dto.getQuantity());
                    stock.setStockStatus(StockStatus.valueOf(dto.getStockStatus()));
                    stock.setStockArrivalDate(String.valueOf(dto.getStockArrivalDate() != null ? LocalDate.parse(dto.getStockArrivalDate()) : LocalDate.now()));
                    stock.setInteriorColour(dto.getInteriorColour());
                    stock.setVinNumber(dto.getVinNumber());
//                    stock.setCreatedAt(LocalDateTime.now());
//                    stock.setUpdatedAt(LocalDateTime.now());
                    return stock;
                })
                .collect(Collectors.toList());

        try {
            List<StockDetails> savedStockDetails = stockDetailsRepository.saveAll(stockDetailsToSave);
            log.info("Successfully saved {} stock entries to database", savedStockDetails.size());
            return new KendoGridResponse<>(savedStockDetails, (long) savedStockDetails.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to save stock details: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to save stock details: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<MddpStock> saveMddpStock(List<MddpStockDTO> dtos) {
        log.info("Saving {} MDDP stock entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty MDDP stock details list");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "MDDP stock details list cannot be empty", null);
        }

        List<MddpStock> mddpStockToSave = dtos.stream()
                .peek(dto -> {
                    if (dto.getVehicleModelId() == null) {
                        log.error("VehicleModel ID is required");
                        throw new IllegalArgumentException("VehicleModel ID is required");
                    }
                    if (dto.getVehicleVariantId() == null) {
                        log.error("VehicleVariant ID is required");
                        throw new IllegalArgumentException("VehicleVariant ID is required");
                    }
                    if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                        log.error("VIN number is required");
                        throw new IllegalArgumentException("VIN number is required");
                    }
                    if (dto.getExpectedDispatchDate() == null) {
                        log.error("Expected dispatch date is required");
                        throw new IllegalArgumentException("Expected dispatch date is required");
                    }
                    if (dto.getExpectedDeliveryDate() == null) {
                        log.error("Expected delivery date is required");
                        throw new IllegalArgumentException("Expected delivery date is required");
                    }
                    if (dto.getStockArrivalDate() != null && !dto.getStockArrivalDate().isEmpty()) {
                        try {
                            LocalDate.parse(dto.getStockArrivalDate()); // Validate date format
                        } catch (DateTimeParseException e) {
                            log.error("Invalid stockArrivalDate format for VIN {}: {}", dto.getVinNumber(), dto.getStockArrivalDate());
                            throw new IllegalArgumentException("Invalid stockArrivalDate format: " + dto.getStockArrivalDate());
                        }
                    } else {
                        log.debug("stockArrivalDate is null for VIN: {}", dto.getVinNumber());
                        // Optionally set default value
                        dto.setStockArrivalDate(String.valueOf(LocalDate.now())); // Default to current date
                    }
                })
                .map(dto -> {
                    MddpStock stock = new MddpStock();
                    stock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
                    stock.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
                    stock.setSuffix(dto.getSuffix());
                    stock.setModelName(dto.getModelName());
                    stock.setFuelType(dto.getFuelType());
                    stock.setColour(dto.getColour());
                    stock.setEngineColour(dto.getEngineColour());
                    stock.setTransmissionType(dto.getTransmissionType());
                    stock.setVariant(dto.getVariant());
                    stock.setQuantity(dto.getQuantity());
                    stock.setStockStatus(StockStatus.valueOf(dto.getStockStatus()));
                    stock.setInteriorColour(dto.getInteriorColour());
                    stock.setVinNumber(dto.getVinNumber());
                    stock.setExpectedDispatchDate(dto.getExpectedDispatchDate());
                    stock.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
                    stock.setStockArrivalDate(dto.getStockArrivalDate() != null ? dto.getStockArrivalDate() : String.valueOf(LocalDate.now()));
                    return stock;
                })
                .collect(Collectors.toList());

        try {
            List<MddpStock> savedMddpStock = mddpStockRepository.saveAll(mddpStockToSave);
            log.info("Successfully saved {} MDDP stock entries to database", savedMddpStock.size());
            return new KendoGridResponse<>(savedMddpStock, (long) savedMddpStock.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to save MDDP stock details: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to save MDDP stock details: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<StockDetails> updateStockDetails(List<StockDetailsDTO> dtos) {
        log.info("Updating {} stock entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty stock details list for update");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Stock details list cannot be empty", null);
        }

        List<StockDetails> updatedStockDetails = new ArrayList<>();

        for (StockDetailsDTO dto : dtos) {
            if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                log.error("VIN number is required for update");
                return new KendoGridResponse<>(Collections.emptyList(), 0L, "VIN number is required", null);
            }

            StockDetails existingStock = (StockDetails) stockDetailsRepository.findByVinNumber(dto.getVinNumber())
                    .orElseThrow(() -> {
                        log.error("Stock not found with VIN: {}", dto.getVinNumber());
                        return new IllegalArgumentException("Stock not found with VIN: " + dto.getVinNumber());
                    });

            if (dto.getVehicleModelId() != null) {
                existingStock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
            }
            if (dto.getModelName() != null) {
                existingStock.setModelName(dto.getModelName());
            }
            if (dto.getVehicleVariantId() != null) {
                existingStock.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
            }
            if (dto.getFuelType() != null) {
                existingStock.setFuelType(dto.getFuelType());
            }
            if (dto.getTransmissionType() != null) {
                existingStock.setTransmissionType(dto.getTransmissionType());
            }
            if (dto.getVariant() != null) {
                existingStock.setVariant(dto.getVariant());
            }
            if (dto.getQuantity() != null) {
                existingStock.setQuantity(dto.getQuantity());
            }
            if (dto.getSuffix() != null) {
                existingStock.setSuffix(dto.getSuffix());
            }
            if (dto.getColour() != null) {
                existingStock.setColour(dto.getColour());
            }
            if (dto.getEngineColour() != null) {
                existingStock.setEngineColour(dto.getEngineColour());
            }
            if (dto.getInteriorColour() != null) {
                existingStock.setInteriorColour(dto.getInteriorColour());
            }

            // existingStock.setUpdatedAt(LocalDateTime.now());
            updatedStockDetails.add(existingStock);
        }

        try {
            List<StockDetails> savedStockDetails = stockDetailsRepository.saveAll(updatedStockDetails);
            log.info("Successfully updated {} stock entries", savedStockDetails.size());
            return new KendoGridResponse<>(savedStockDetails, (long) savedStockDetails.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to update stock details: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to update stock details: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<MddpStock> updateMddpStock(List<MddpStockDTO> dtos) {
        log.info("Updating {} MDDP stock entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty MDDP stock details list for update");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "MDDP stock details list cannot be empty", null);
        }

        List<MddpStock> updatedMddpStockDetails = new ArrayList<>();

        for (MddpStockDTO dto : dtos) {
            if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                log.error("VIN number is required for update");
                return new KendoGridResponse<>(Collections.emptyList(), 0L, "VIN number is required", null);
            }

            MddpStock existingStock = mddpStockRepository.findByVinNumber(dto.getVinNumber())
                    .orElseThrow(() -> {
                        log.error("MDDP stock not found with VIN: {}", dto.getVinNumber());
                        return new IllegalArgumentException("MDDP stock not found with VIN: " + dto.getVinNumber());
                    });

            if (dto.getVehicleModelId() != null) {
                existingStock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
            }
            if (dto.getVehicleVariantId() != null) {
                existingStock.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
            }
            if (dto.getModelName() != null) {
                existingStock.setModelName(dto.getModelName());
            }
            if (dto.getFuelType() != null) {
                existingStock.setFuelType(dto.getFuelType());
            }
            if (dto.getTransmissionType() != null) {
                existingStock.setTransmissionType(dto.getTransmissionType());
            }
            if (dto.getVariant() != null) {
                existingStock.setVariant(dto.getVariant());
            }
            if (dto.getQuantity() != null) {
                existingStock.setQuantity(dto.getQuantity());
            }
            if (dto.getSuffix() != null) {
                existingStock.setSuffix(dto.getSuffix());
            }
            if (dto.getColour() != null) {
                existingStock.setColour(dto.getColour());
            }
            if (dto.getEngineColour() != null) {
                existingStock.setEngineColour(dto.getEngineColour());
            }
            if (dto.getInteriorColour() != null) {
                existingStock.setInteriorColour(dto.getInteriorColour());
            }
            if (dto.getStockStatus() != null) {
                existingStock.setStockStatus(StockStatus.valueOf(dto.getStockStatus()));
            }
            if (dto.getExpectedDispatchDate() != null) {
                existingStock.setExpectedDispatchDate(dto.getExpectedDispatchDate());
            }
            if (dto.getExpectedDeliveryDate() != null) {
                existingStock.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
            }

            updatedMddpStockDetails.add(existingStock);
        }

        try {
            List<MddpStock> savedMddpStockDetails = mddpStockRepository.saveAll(updatedMddpStockDetails);
            log.info("Successfully updated {} MDDP stock entries", savedMddpStockDetails.size());
            return new KendoGridResponse<>(savedMddpStockDetails, (long) savedMddpStockDetails.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to update MDDP stock details: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to update MDDP stock details: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<ManufacturerOrder> saveManufacturerOrders(List<ManufacturerOrderDTO> dtos) {
        log.info("Saving {} manufacturer order entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty manufacturer order list");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Manufacturer order list cannot be empty", null);
        }

        List<ManufacturerOrder> ordersToSave = dtos.stream()
                .peek(dto -> {
                    if (dto.getVehicleVariantId() == null) {
                        log.error("VehicleVariant ID is required");
                        throw new IllegalArgumentException("VehicleVariant ID is required");
                    }
                    if (dto.getManufacturerLocation() == null || dto.getManufacturerLocation().trim().isEmpty()) {
                        log.error("Manufacturer location is required");
                        throw new IllegalArgumentException("Manufacturer location is required");
                    }
                    if (dto.getOrderStatus() == null || dto.getOrderStatus().trim().isEmpty()) {
                        log.error("Order status is required");
                        throw new IllegalArgumentException("Order status is required");
                    }
                    if (dto.getVinNumber() != null && !dto.getVinNumber().trim().isEmpty()) {
                        if (manufacturerOrderRepository.findByVinNumber(dto.getVinNumber()).isPresent()) {
                            log.error("Duplicate VIN number: {}", dto.getVinNumber());
                            throw new IllegalArgumentException("VIN number already exists: " + dto.getVinNumber());
                        }
                    }
                })
                .map(dto -> {
                    ManufacturerOrder order = new ManufacturerOrder();
                    order.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
                    order.setManufacturerLocation(dto.getManufacturerLocation());
                    order.setOrderStatus(OrderStatus.valueOf(dto.getOrderStatus()));
                    order.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
                    order.setModelName(dto.getModelName());
                    order.setFuelType(dto.getFuelType());
                    order.setColour(dto.getColour());
                    order.setVariant(dto.getVariant());
                    order.setVinNumber(dto.getVinNumber());
                    order.setSuffix(dto.getSuffix());
                    order.setInteriorColour(dto.getInteriorColour());
                    order.setEngineColour(dto.getEngineColour());
                    order.setTransmissionType(dto.getTransmissionType());
                    return order;
                })
                .collect(Collectors.toList());

        try {
            List<ManufacturerOrder> savedOrders = manufacturerOrderRepository.saveAll(ordersToSave);
            log.info("Successfully saved {} manufacturer order entries to database", savedOrders.size());
            return new KendoGridResponse<>(savedOrders, (long) savedOrders.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to save manufacturer orders: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to save manufacturer orders: " + e.getMessage(), null);
        }
    }

    private VehicleModel getVehicleModelEntity(Long modelId) {
        return vehicleModelRepository.findById(modelId)
                .orElseThrow(() -> {
                    log.error("VehicleModel not found with ID: {}", modelId);
                    return new IllegalArgumentException("Invalid VehicleModel ID: " + modelId);
                });
    }

    private VehicleVariant getVehicleVariantEntity(Long variantId) {
        return vehicleVariantRepository.findById(variantId)
                .orElseThrow(() -> {
                    log.error("VehicleVariant not found with ID: {}", variantId);
                    return new IllegalArgumentException("Invalid VehicleVariant ID: " + variantId);
                });
    }

    public List<StockDetailsDTO> getAllStockDetails() {
        return stockDetailsRepository.findAll().stream().map(stock -> {
            StockDetailsDTO dto = new StockDetailsDTO();
            dto.setStockId(stock.getStockId());
            dto.setVehicleModelId(stock.getVehicleModelId().getVehicleModelId());
            dto.setVehicleVariantId(stock.getVehicleVariantId().getVehicleVariantId());
            dto.setModelName(stock.getModelName());
            dto.setVariant(stock.getVariant());
            dto.setColour(stock.getColour());
            dto.setEngineColour(stock.getEngineColour());
            dto.setInteriorColour(stock.getInteriorColour());
            dto.setFuelType(stock.getFuelType());
            dto.setTransmissionType(stock.getTransmissionType());
            dto.setQuantity(stock.getQuantity());
            dto.setVinNumber(stock.getVinNumber());
            dto.setStockStatus(stock.getStockStatus() != null ? stock.getStockStatus().name() : null);
            dto.setSuffix(stock.getSuffix());
            return dto;
        }).collect(Collectors.toList());
    }

    public KendoGridResponse<MddpStockDTO> getAllMddpStock() {
        List<MddpStockDTO> dtoList = mddpStockRepository.findAll().stream().map(stock -> {
            MddpStockDTO dto = new MddpStockDTO();
            dto.setMddpId(stock.getMddpId());
            dto.setVehicleModelId(stock.getVehicleModelId().getVehicleModelId());
            dto.setVehicleVariantId(stock.getVehicleVariantId().getVehicleVariantId());
            dto.setVariant(stock.getVariant());
            dto.setModelName(stock.getModelName());
            dto.setSuffix(stock.getSuffix());
            dto.setColour(stock.getColour());
            dto.setEngineColour(stock.getEngineColour());
            dto.setInteriorColour(stock.getInteriorColour());
            dto.setFuelType(stock.getFuelType());
            dto.setTransmissionType(stock.getTransmissionType());
            dto.setQuantity(stock.getQuantity());
            dto.setVinNumber(stock.getVinNumber());
            dto.setStockStatus(stock.getStockStatus() != null ? stock.getStockStatus().name() : null);
            dto.setExpectedDispatchDate(stock.getExpectedDispatchDate());
            dto.setExpectedDeliveryDate(stock.getExpectedDeliveryDate());
            return dto;
        }).collect(Collectors.toList());

        return new KendoGridResponse<>(dtoList, dtoList.size(), null, null);
    }

    public KendoGridResponse<FinanceDTO> getAllFinanceDetails() {
        List<FinanceDTO> dtoList = financeDetailsRepository.findAll().stream().map(entity -> {
            FinanceDTO dto = new FinanceDTO();
            dto.setFinanceId(entity.getFinanceId());
            dto.setCustomerOrderId(entity.getCustomerOrderId());
            dto.setCustomerName(entity.getCustomerName());
            dto.setFinanceStatus(entity.getFinanceStatus() != null ? entity.getFinanceStatus().name() : null);
            dto.setApprovedBy(entity.getApprovedBy());
            dto.setRejectedBy(entity.getRejectedBy());
            return dto;
        }).collect(Collectors.toList());

        return new KendoGridResponse<>(dtoList, dtoList.size(), null, null);
    }

    public KendoGridResponse<ManufacturerOrderDTO> getAllManufacturerOrders() {
        log.info("Retrieving all manufacturer orders");

        try {
            List<ManufacturerOrder> orders = manufacturerOrderRepository.findAll();
            List<ManufacturerOrderDTO> dtos = orders.stream()
                    .map(this::mapToManufacturerOrderDTO)
                    .collect(Collectors.toList());

            log.info("Successfully retrieved {} manufacturer orders", dtos.size());
            return new KendoGridResponse<>(dtos, (long) dtos.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to retrieve manufacturer orders: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Error retrieving manufacturer orders: " + e.getMessage(), null);
        }
    }

    @Transactional
    public KendoGridResponse<ManufacturerOrder> updateManufacturerOrders(List<ManufacturerOrderDTO> dtos) {
        log.info("Updating {} manufacturer order entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty manufacturer order list for update");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Manufacturer order list cannot be empty", null);
        }

        List<ManufacturerOrder> updatedOrders = new ArrayList<>();

        for (ManufacturerOrderDTO dto : dtos) {
            if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                log.error("VIN number is required for update");
                return new KendoGridResponse<>(Collections.emptyList(), 0L, "VIN number is required", null);
            }

            ManufacturerOrder existingOrder = manufacturerOrderRepository.findByVinNumber(dto.getVinNumber())
                    .orElseThrow(() -> {
                        log.error("Manufacturer order not found with VIN: {}", dto.getVinNumber());
                        return new IllegalArgumentException("Manufacturer order not found with VIN: " + dto.getVinNumber());
                    });

            if (dto.getManufacturerId() != null) {
                existingOrder.setManufacturerId(dto.getManufacturerId());
            }
            if (dto.getVehicleVariantId() != null) {
                existingOrder.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
            }
            if (dto.getManufacturerLocation() != null) {
                existingOrder.setManufacturerLocation(dto.getManufacturerLocation());
            }
            if (dto.getOrderStatus() != null) {
                existingOrder.setOrderStatus(OrderStatus.valueOf(dto.getOrderStatus()));
            }
            if (dto.getEstimatedArrivalDate() != null) {
                existingOrder.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
            }
            if (dto.getModelName() != null) {
                existingOrder.setModelName(dto.getModelName());
            }
            if (dto.getFuelType() != null) {
                existingOrder.setFuelType(dto.getFuelType());
            }
            if (dto.getColour() != null) {
                existingOrder.setColour(dto.getColour());
            }
            if (dto.getVariant() != null) {
                existingOrder.setVariant(dto.getVariant());
            }
            if (dto.getSuffix() != null) {
                existingOrder.setSuffix(dto.getSuffix());
            }
            if (dto.getInteriorColour() != null) {
                existingOrder.setInteriorColour(dto.getInteriorColour());
            }
            if (dto.getEngineColour() != null) {
                existingOrder.setEngineColour(dto.getEngineColour());
            }
            if (dto.getTransmissionType() != null) {
                existingOrder.setTransmissionType(dto.getTransmissionType());
            }

            updatedOrders.add(existingOrder);
        }

        try {
            List<ManufacturerOrder> savedOrders = manufacturerOrderRepository.saveAll(updatedOrders);
            log.info("Successfully updated {} manufacturer order entries", savedOrders.size());
            return new KendoGridResponse<>(savedOrders, (long) savedOrders.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to update manufacturer orders: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to update manufacturer orders: " + e.getMessage(), null);
        }
    }

    private ManufacturerOrderDTO mapToManufacturerOrderDTO(ManufacturerOrder order) {
        ManufacturerOrderDTO dto = new ManufacturerOrderDTO();
        dto.setManufacturerId(order.getManufacturerId());
        dto.setVehicleVariantId(order.getVehicleVariantId() != null ? order.getVehicleVariantId().getVehicleVariantId() : null);
        dto.setManufacturerLocation(order.getManufacturerLocation());
        dto.setModelName(order.getModelName());
        dto.setFuelType(order.getFuelType());
        dto.setColour(order.getColour());
        dto.setVariant(order.getVariant());
        dto.setVinNumber(order.getVinNumber());
        dto.setSuffix(order.getSuffix());
        dto.setInteriorColour(order.getInteriorColour());
        dto.setEngineColour(order.getEngineColour());
        dto.setTransmissionType(order.getTransmissionType());
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        dto.setEstimatedArrivalDate(order.getEstimatedArrivalDate());
        return dto;
    }

    public KendoGridResponse<VehicleVariant> getAllVehicleVariants() {
        List<VehicleVariant> variants = vehicleVariantRepository.findAll();
        return new KendoGridResponse<>(variants, variants.size(), null, null);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            log.warn("Cannot parse null or empty value");
            return null;
        }
        try {
            // Extract numeric part by removing non-digit characters (except for negative signs if needed)
            String numericPart = value.replaceAll("[^0-9-]", "");
            if (numericPart.isEmpty()) {
                log.warn("No numeric value found in: {}", value);
                return null;
            }
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer: {}", value, e);
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse double: {}", value, e);
            return null;
        }
    }

    public StockDetailsDTO getStockDetailByModelAndVariant(String modelName, Long vehicleVariantId) {
        log.info("Fetching stock detail for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
        try {
            if (modelName == null || modelName.trim().isEmpty()) {
                log.error("Model name cannot be empty for fetching stock details");
                throw new IllegalArgumentException("Model name cannot be empty");
            }
            if (vehicleVariantId == null) {
                log.error("Vehicle variant ID cannot be null for fetching stock details");
                throw new IllegalArgumentException("Vehicle variant ID cannot be null");
            }
            Optional<StockDetails> stockDetail = stockDetailsRepository.findByModelNameAndVehicleVariantIdVariantId(modelName, vehicleVariantId);
            if (stockDetail.isEmpty()) {
                log.warn("No stock detail found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                return null;
            }
            StockDetailsDTO dto = new StockDetailsDTO();
            dto.setStockId(stockDetail.get().getStockId());
            dto.setVehicleModelId(stockDetail.get().getVehicleModelId().getVehicleModelId());
            dto.setVehicleVariantId(stockDetail.get().getVehicleVariantId().getVehicleVariantId());
            dto.setModelName(stockDetail.get().getModelName());
            dto.setVariant(stockDetail.get().getVariant());
            dto.setColour(stockDetail.get().getColour());
            dto.setEngineColour(stockDetail.get().getEngineColour());
            dto.setInteriorColour(stockDetail.get().getInteriorColour());
            dto.setFuelType(stockDetail.get().getFuelType());
            dto.setTransmissionType(stockDetail.get().getTransmissionType());
            dto.setQuantity(stockDetail.get().getQuantity());
            dto.setVinNumber(stockDetail.get().getVinNumber());
            dto.setStockStatus(stockDetail.get().getStockStatus() != null ? stockDetail.get().getStockStatus().name() : null);
            dto.setSuffix(stockDetail.get().getSuffix());
            log.info("Successfully retrieved stock detail for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
            return dto;
        } catch (IllegalArgumentException e) {
            log.error("Validation error while fetching stock detail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch stock detail: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching stock detail for modelName: {} and vehicleVariantId: {}: {}", modelName, vehicleVariantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch stock detail: " + e.getMessage(), e);
        }
    }

    public MddpStockDTO getMddpStockByModelAndVariant(String modelName, Long vehicleVariantId) {
        log.info("Fetching MDDP stock detail for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
        try {
            if (modelName == null || modelName.trim().isEmpty()) {
                log.error("Model name cannot be empty for fetching MDDP stock details");
                throw new IllegalArgumentException("Model name cannot be empty");
            }
            if (vehicleVariantId == null) {
                log.error("Vehicle variant ID cannot be null for fetching MDDP stock details");
                throw new IllegalArgumentException("Vehicle variant ID cannot be null");
            }
            Optional<MddpStock> mddpStock = mddpStockRepository.findByModelNameAndVehicleVariantIdVariantId(modelName, vehicleVariantId);
            if (mddpStock.isEmpty()) {
                log.warn("No MDDP stock detail found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                return null;
            }
            MddpStockDTO dto = new MddpStockDTO();
            dto.setMddpId(mddpStock.get().getMddpId());
            dto.setVehicleModelId(mddpStock.get().getVehicleModelId().getVehicleModelId());
            dto.setVehicleVariantId(mddpStock.get().getVehicleVariantId().getVehicleVariantId());
            dto.setVariant(mddpStock.get().getVariant());
            dto.setModelName(mddpStock.get().getModelName());
            dto.setSuffix(mddpStock.get().getSuffix());
            dto.setColour(mddpStock.get().getColour());
            dto.setEngineColour(mddpStock.get().getEngineColour());
            dto.setInteriorColour(mddpStock.get().getInteriorColour());
            dto.setFuelType(mddpStock.get().getFuelType());
            dto.setTransmissionType(mddpStock.get().getTransmissionType());
            dto.setQuantity(mddpStock.get().getQuantity());
            dto.setVinNumber(mddpStock.get().getVinNumber());
            dto.setStockStatus(mddpStock.get().getStockStatus() != null ? mddpStock.get().getStockStatus().name() : null);
            dto.setExpectedDispatchDate(mddpStock.get().getExpectedDispatchDate());
            dto.setExpectedDeliveryDate(mddpStock.get().getExpectedDeliveryDate());
            log.info("Successfully retrieved MDDP stock detail for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
            return dto;
        } catch (IllegalArgumentException e) {
            log.error("Validation error while fetching MDDP stock detail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch MDDP stock detail: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching MDDP stock detail for modelName: {} and vehicleVariantId: {}: {}", modelName, vehicleVariantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch MDDP stock detail: " + e.getMessage(), e);
        }
    }

    public ManufacturerOrderDTO getManufacturerOrderByModelAndVariant(String modelName, Long vehicleVariantId) {
        log.info("Fetching manufacturer order for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
        try {
            if (modelName == null || modelName.trim().isEmpty()) {
                log.error("Model name cannot be empty for fetching manufacturer orders");
                throw new IllegalArgumentException("Model name cannot be empty");
            }
            if (vehicleVariantId == null) {
                log.error("Vehicle variant ID cannot be null for fetching manufacturer orders");
                throw new IllegalArgumentException("Vehicle variant ID cannot be null");
            }
            Optional<ManufacturerOrder> manufacturerOrder = manufacturerOrderRepository.findByModelNameAndVehicleVariantIdVariantId(modelName, vehicleVariantId);
            if (manufacturerOrder.isEmpty()) {
                log.warn("No manufacturer order found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                return null;
            }
            ManufacturerOrderDTO dto = mapToManufacturerOrderDTO(manufacturerOrder.get());
            log.info("Successfully retrieved manufacturer order for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
            return dto;
        } catch (IllegalArgumentException e) {
            log.error("Validation error while fetching manufacturer order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch manufacturer order: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching manufacturer order for modelName: {} and vehicleVariantId: {}: {}", modelName, vehicleVariantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch manufacturer order: " + e.getMessage(), e);
        }
    }

    public VehicleVariantDTO getVehicleVariantByModelAndVariant(String modelName, Long vehicleVariantId) {
        log.info("Fetching vehicle variant for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
        try {
            if (modelName == null || modelName.trim().isEmpty()) {
                log.error("Model name cannot be empty for fetching vehicle variants");
                throw new IllegalArgumentException("Model name cannot be empty");
            }
            if (vehicleVariantId == null) {
                log.error("Vehicle variant ID cannot be null for fetching vehicle variants");
                throw new IllegalArgumentException("Vehicle variant ID cannot be null");
            }
            Optional<VehicleVariant> vehicleVariant = vehicleVariantRepository.findByModelNameAndVehicleVariantId(modelName, vehicleVariantId);
            if (vehicleVariant.isEmpty()) {
                log.warn("No vehicle variant found for modelName: {} and vehicleVariantId: {}", modelName, vehicleVariantId);
                return null;
            }
            VehicleVariantDTO dto = new VehicleVariantDTO();
            VehicleVariant variant = vehicleVariant.get();
            dto.setVehicleModelId(variant.getVehicleModelId().getVehicleModelId());
            dto.setModelName(variant.getModelName());
            dto.setVariant(variant.getVariant());
            dto.setSuffix(variant.getSuffix());
            dto.setSafetyFeature(variant.getSafetyFeature());
            dto.setColour(variant.getColour());
            dto.setEngineColour(variant.getEngineColour());
            dto.setTransmissionType(variant.getTransmissionType());
            dto.setInteriorColour(variant.getInteriorColour());
            dto.setVinNumber(variant.getVinNumber());
            dto.setEngineCapacity(variant.getEngineCapacity());
            dto.setFuelType(variant.getFuelType());
            dto.setPrice(variant.getPrice());
            dto.setYearOfManufacture(variant.getYearOfManufacture());
            dto.setBodyType(variant.getBodyType());
            dto.setFuelTankCapacity(variant.getFuelTankCapacity());
            dto.setSeatingCapacity(variant.getSeatingCapacity());
            dto.setMaxPower(variant.getMaxPower());
            dto.setMaxTorque(variant.getMaxTorque());
            dto.setTopSpeed(variant.getTopSpeed());
            dto.setWheelBase(variant.getWheelBase());
            dto.setWidth(variant.getWidth());
            dto.setLength(variant.getLength());
            dto.setInfotainment(variant.getInfotainment());
            dto.setComfort(variant.getComfort());
            dto.setNumberOfAirBags(variant.getNumberOfAirBags());
            dto.setMileageCity(variant.getMileageCity());
            dto.setMileageHighway(variant.getMileageHighway());
            log.info("Successfully retrieved vehicle variant for modelName: {} and vehicleVariantId: {} at {}", modelName, vehicleVariantId, LocalDateTime.now());
            return dto;
        } catch (IllegalArgumentException e) {
            log.error("Validation error while fetching vehicle variant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch vehicle variant: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error fetching vehicle variant for modelName: {} and vehicleVariantId: {}: {}", modelName, vehicleVariantId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch vehicle variant: " + e.getMessage(), e);
        }
    }
}