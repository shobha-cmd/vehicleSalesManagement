package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.dto.apiresponse.*;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.enums.StockStatus;
import com.vehicle.salesmanagement.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    // Unchanged: Returns VehicleAttributesResponse as per original
    public VehicleAttributesResponse getDropdownData(String modelName, String variant) {
        log.info("Fetching dropdown data at {}", LocalDateTime.now());

        VehicleAttributesResponse response = new VehicleAttributesResponse();

        // Fetch all model names
        List<String> modelNames = vehicleModelRepository.findAll().stream()
                .map(model -> model.getModelName() != null ? model.getModelName() : "")
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        response.setModelNames(modelNames);

        // Initialize model details map
        Map<String, VehicleAttributesResponse.ModelAttributes> modelDetails = new HashMap<>();
        response.setModelDetails(modelDetails);

        // Fetch variants based on modelName and variant
        List<VehicleVariant> variants;
        if (modelName != null && variant != null) {
            variants = vehicleVariantRepository.findByVehicleModelId_ModelNameAndVariant(modelName, variant);
        } else if (modelName != null) {
            variants = vehicleVariantRepository.findByVehicleModelId_ModelName(modelName);
        } else {
            variants = vehicleVariantRepository.findAll();
        }

        // Handle empty variants for specific model or model+variant
        if (variants.isEmpty() && modelName != null) {
            VehicleAttributesResponse.ModelAttributes emptyAttributes = new VehicleAttributesResponse.ModelAttributes();
            if (variant != null) {
                emptyAttributes.setVariants(Collections.singletonList(variant));
            }
            modelDetails.put(modelName, emptyAttributes);
            return response;
        }

        // Group variants by model name
        Map<String, List<VehicleVariant>> variantsByModel = variants.stream()
                .filter(v -> v.getVehicleModelId() != null && v.getVehicleModelId().getModelName() != null)
                .collect(Collectors.groupingBy(v -> v.getVehicleModelId().getModelName()));

        // Populate attributes for each model
        variantsByModel.forEach((mName, modelVariants) -> {
            VehicleAttributesResponse.ModelAttributes attributes = new VehicleAttributesResponse.ModelAttributes();

            attributes.setVariants(modelVariants.stream()
                    .map(VehicleVariant::getVariant)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setColours(modelVariants.stream()
                    .map(VehicleVariant::getColour)
                    .filter(Objects::nonNull)
                    .flatMap(colour -> Arrays.stream(colour.split(",\\s*")))
                    .filter(colour -> !colour.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setEngineColours(modelVariants.stream()
                    .map(VehicleVariant::getEngineColour)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setInteriorColours(modelVariants.stream()
                    .map(VehicleVariant::getInteriorColour)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setFuelTypes(modelVariants.stream()
                    .map(VehicleVariant::getFuelType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setTransmissionTypes(modelVariants.stream()
                    .map(VehicleVariant::getTransmissionType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setPrices(modelVariants.stream()
                    .map(VehicleVariant::getPrice)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setYearsOfManufacture(modelVariants.stream()
                    .map(VehicleVariant::getYearOfManufacture)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setBodyTypes(modelVariants.stream()
                    .map(VehicleVariant::getBodyType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setFuelTankCapacities(modelVariants.stream()
                    .map(VehicleVariant::getFuelTankCapacity)
                    .filter(Objects::nonNull)
                    .map(this::parseDouble)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setNumberOfAirbags(modelVariants.stream()
                    .map(VehicleVariant::getNumberOfAirBags)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMileageCities(modelVariants.stream()
                    .map(VehicleVariant::getMileageCity)
                    .filter(Objects::nonNull)
                    .map(this::parseDouble)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMileageHighways(modelVariants.stream()
                    .map(VehicleVariant::getMileageHighway)
                    .filter(Objects::nonNull)
                    .map(this::parseDouble)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setSeatingCapacities(modelVariants.stream()
                    .map(VehicleVariant::getSeatingCapacity)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMaxPowers(modelVariants.stream()
                    .map(VehicleVariant::getMaxPower)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setMaxTorques(modelVariants.stream()
                    .map(VehicleVariant::getMaxTorque)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setTopSpeeds(modelVariants.stream()
                    .map(VehicleVariant::getTopSpeed)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setWheelBases(modelVariants.stream()
                    .map(VehicleVariant::getWheelBase)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setWidths(modelVariants.stream()
                    .map(VehicleVariant::getWidth)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setLengths(modelVariants.stream()
                    .map(VehicleVariant::getLength)
                    .filter(Objects::nonNull)
                    .map(this::parseInteger)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setSafetyFeatures(modelVariants.stream()
                    .map(VehicleVariant::getSafetyFeature)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setInfotainments(modelVariants.stream()
                    .map(VehicleVariant::getInfotainment)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setComforts(modelVariants.stream()
                    .map(VehicleVariant::getComfort)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setSuffixes(modelVariants.stream()
                    .map(VehicleVariant::getSuffix)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            attributes.setEngineCapacities(modelVariants.stream()
                    .map(VehicleVariant::getEngineCapacity)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList()));

            modelDetails.put(mName, attributes);
        });

        log.info("Successfully fetched dropdown data");
        return response;
    }

    // Helper methods for parsing
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                String str = ((String) value).trim();
                if (str.isEmpty()) {
                    return null;
                }
                str = str.replaceAll("[^0-9.]", "");
                if (str.isEmpty()) {
                    return null;
                }
                return Double.valueOf(str).intValue();
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            log.warn("Unsupported type for integer parsing: {}", value.getClass());
        } catch (NumberFormatException e) {
            log.error("Failed to parse integer from value: {}, error: {}", value, e.getMessage());
        }
        return null;
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof String) {
                String str = ((String) value).trim();
                if (str.isEmpty()) {
                    return null;
                }
                str = str.replaceAll("[^0-9.]", "");
                if (str.isEmpty()) {
                    return null;
                }
                return Double.valueOf(str);
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            log.warn("Unsupported type for double parsing: {}", value.getClass());
        } catch (NumberFormatException e) {
            log.error("Failed to parse double from value: {}, error: {}", value, e.getMessage());
        }
        return null;
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
                    model.setCreatedAt(LocalDateTime.now());
                    model.setUpdatedAt(LocalDateTime.now());
                    model.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "admin");
                    model.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "admin");
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
    public VehicleModel saveVehicleModel(VehicleModelDTO dto) {
        log.info("Starting insertion of single vehicle model at {}", LocalDateTime.now());

        if (dto == null) {
            log.error("Received null DTO");
            throw new IllegalArgumentException("Vehicle model DTO cannot be null");
        }

        if (dto.getModelName() == null || dto.getModelName().trim().isEmpty()) {
            log.error("Invalid modelName in DTO: {}", dto);
            throw new IllegalArgumentException("modelName cannot be null or empty");
        }

        if (vehicleModelRepository.findByModelName(dto.getModelName()).isPresent()) {
            log.error("Model already exists: {}", dto.getModelName());
            throw new IllegalArgumentException("Vehicle model already exists");
        }

        VehicleModel model = new VehicleModel();
        model.setModelName(dto.getModelName());
        model.setCreatedAt(LocalDateTime.now());
        model.setUpdatedAt(LocalDateTime.now());
        model.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "admin");
        model.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "admin");

        try {
            VehicleModel savedModel = vehicleModelRepository.save(model);
            log.info("Successfully saved vehicle model: {}", savedModel.getModelName());
            return savedModel;
        } catch (Exception e) {
            log.error("Failed to save vehicle model: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save vehicle model: " + e.getMessage(), e);
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
                    variant.setCreatedAt(LocalDateTime.now());
                    variant.setUpdatedAt(LocalDateTime.now());
                    variant.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
                    variant.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");
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
            // Validate required fields
            if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                log.error("VIN number is required for update");
                return new KendoGridResponse<>(Collections.emptyList(), 0L, "VIN number is required", null);
            }

            // Find existing VehicleVariant by VIN
            VehicleVariant existingVariant = vehicleVariantRepository.findByVinNumber(dto.getVinNumber())
                    .orElseThrow(() -> {
                        log.error("VehicleVariant not found with VIN: {}", dto.getVinNumber());
                        return new IllegalArgumentException("VehicleVariant not found with VIN: " + dto.getVinNumber());
                    });

            // Update fields from DTO
            if (dto.getVehicleModelId() != null) {
                existingVariant.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
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

            // Update audit fields
            existingVariant.setUpdatedAt(LocalDateTime.now());
            existingVariant.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");

            updatedVehicleVariants.add(existingVariant);
        }

        try {
            List<VehicleVariant> savedVehicleVariants = vehicleVariantRepository.saveAll(updatedVehicleVariants);
            log.info("Successfully updated {} vehicle variant entries", savedVehicleVariants.size());
            return new KendoGridResponse<>(savedVehicleVariants, (long) savedVehicleVariants.size(), null, null);
        } catch (Exception e) {
            log.error("Failed to update vehicle variants: {}", e.getMessage(), e);
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Failed to update vehicle variants: " + e.getMessage(), null);
        }
    }

    @Transactional
    public VehicleVariant saveVehicleVariant(VehicleVariantDTO dto) {
        log.info("Starting insertion of single vehicle variant at {}", LocalDateTime.now());

        if (dto == null) {
            log.error("Received null DTO");
            throw new IllegalArgumentException("Vehicle variant DTO cannot be null");
        }

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

        if (vehicleVariantRepository.findByVinNumber(dto.getVinNumber()).isPresent()) {
            log.error("Duplicate VIN: {}", dto.getVinNumber());
            throw new IllegalArgumentException("VIN number already exists");
        }

        VehicleVariant variant = new VehicleVariant();
        variant.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
        variant.setVariant(dto.getVariant());
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
        variant.setCreatedAt(LocalDateTime.now());
        variant.setUpdatedAt(LocalDateTime.now());
        variant.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
        variant.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");

        try {
            VehicleVariant savedVariant = vehicleVariantRepository.save(variant);
            log.info("Successfully saved vehicle variant: {}", savedVariant.getVariant());
            return savedVariant;
        } catch (Exception e) {
            log.error("Failed to save vehicle variant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save vehicle variant: " + e.getMessage(), e);
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
                    stock.setFuelType(dto.getFuelType());
                    stock.setColour(dto.getColour());
                    stock.setEngineColour(dto.getEngineColour());
                    stock.setTransmissionType(dto.getTransmissionType());
                    stock.setVariant(dto.getVariant());
                    stock.setQuantity(dto.getQuantity());
                    stock.setStockStatus(StockStatus.valueOf(dto.getStockStatus()));
                    stock.setInteriorColour(dto.getInteriorColour());
                    stock.setVinNumber(dto.getVinNumber());
                    stock.setCreatedAt(LocalDateTime.now());
                    stock.setUpdatedAt(LocalDateTime.now());
                    stock.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
                    stock.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");
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
    public StockDetails saveStockDetail(StockDetailsDTO dto) {
        log.info("Starting insertion of single stock detail at {}", LocalDateTime.now());

        if (dto == null) {
            log.error("Received null DTO");
            throw new IllegalArgumentException("Stock details DTO cannot be null");
        }

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

        StockDetails stock = new StockDetails();
        stock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
        stock.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
        stock.setSuffix(dto.getSuffix());
        stock.setFuelType(dto.getFuelType());
        stock.setColour(dto.getColour());
        stock.setEngineColour(dto.getEngineColour());
        stock.setTransmissionType(dto.getTransmissionType());
        stock.setVariant(dto.getVariant());
        stock.setQuantity(dto.getQuantity());
        stock.setStockStatus(StockStatus.valueOf(dto.getStockStatus()));
        stock.setInteriorColour(dto.getInteriorColour());
        stock.setVinNumber(dto.getVinNumber());
        stock.setCreatedAt(LocalDateTime.now());
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
        stock.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");

        try {
            StockDetails savedStock = stockDetailsRepository.save(stock);
            log.info("Successfully saved stock detail for VIN: {}", savedStock.getVinNumber());
            return savedStock;
        } catch (Exception e) {
            log.error("Failed to save stock detail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save stock detail: " + e.getMessage(), e);
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
                })
                .map(dto -> {
                    MddpStock stock = new MddpStock();
                    stock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
                    stock.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
                    stock.setSuffix(dto.getSuffix());
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
                    stock.setCreatedAt(LocalDateTime.now());
                    stock.setUpdatedAt(LocalDateTime.now());
                    stock.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
                    stock.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");
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
    public MddpStock saveMddpStock(MddpStockDTO dto) {
        log.info("Starting insertion of single MDDP stock at {}", LocalDateTime.now());

        if (dto == null) {
            log.error("Received null DTO");
            throw new IllegalArgumentException("MDDP stock DTO cannot be null");
        }

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

        MddpStock stock = new MddpStock();
        stock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
        stock.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
        stock.setSuffix(dto.getSuffix());
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
        stock.setCreatedAt(LocalDateTime.now());
        stock.setUpdatedAt(LocalDateTime.now());
        stock.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
        stock.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");

        try {
            MddpStock savedMddpStock = mddpStockRepository.save(stock);
            log.info("Successfully saved MDDP stock for VIN: {}", savedMddpStock.getVinNumber());
            return savedMddpStock;
        } catch (Exception e) {
            log.error("Failed to save MDDP stock: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save MDDP stock: " + e.getMessage(), e);
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
                })
                .map(dto -> {
                    ManufacturerOrder order = new ManufacturerOrder();
                    order.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
                    order.setManufacturerLocation(dto.getManufacturerLocation());
                    order.setOrderStatus(OrderStatus.valueOf(dto.getOrderStatus()));
                    order.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
                    order.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
                    order.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");
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
    @Transactional
    public KendoGridResponse<StockDetails> updateStockDetails(List<StockDetailsDTO> dtos) {
        log.info("Updating {} stock entries", dtos != null ? dtos.size() : 0);

        if (dtos == null || dtos.isEmpty()) {
            log.error("Received empty stock details list for update");
            return new KendoGridResponse<>(Collections.emptyList(), 0L, "Stock details list cannot be empty", null);
        }

        List<StockDetails> updatedStockDetails = new ArrayList<>();

        for (StockDetailsDTO dto : dtos) {
            // Validate required fields
            if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                log.error("VIN number is required for update");
                return new KendoGridResponse<>(Collections.emptyList(), 0L, "VIN number is required", null);
            }

            // Find existing stock by VIN
            StockDetails existingStock = (StockDetails) stockDetailsRepository.findByVinNumber(dto.getVinNumber())
                    .orElseThrow(() -> {
                        log.error("Stock not found with VIN: {}", dto.getVinNumber());
                        return new IllegalArgumentException("Stock not found with VIN: " + dto.getVinNumber());
                    });

            // Update fields from DTO (based on screenshot fields)
            if (dto.getVehicleModelId() != null) {
                existingStock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
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
            if (dto.getVariant() != null) { // Grade is mapped to Variant in the DTO
                existingStock.setVariant(dto.getVariant());
            }
            if (dto.getQuantity() != null) {
                existingStock.setQuantity(dto.getQuantity());
            }
            if (dto.getSuffix() != null) {
                existingStock.setSuffix(dto.getSuffix());
            }
            if (dto.getColour() != null) { // Color
                existingStock.setColour(dto.getColour());
            }
            if (dto.getEngineColour() != null) { // Exterior Color (E.Color)
                existingStock.setEngineColour(dto.getEngineColour());
            }
            if (dto.getInteriorColour() != null) {
                existingStock.setInteriorColour(dto.getInteriorColour());
            }

            // Update audit fields
            existingStock.setUpdatedAt(LocalDateTime.now());
            existingStock.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");

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
            // Validate required fields
            if (dto.getVinNumber() == null || dto.getVinNumber().trim().isEmpty()) {
                log.error("VIN number is required for update");
                return new KendoGridResponse<>(Collections.emptyList(), 0L, "VIN number is required", null);
            }

            // Find existing MDDP stock by VIN
            MddpStock existingStock = (MddpStock) mddpStockRepository.findByVinNumber(dto.getVinNumber())
                    .orElseThrow(() -> {
                        log.error("MDDP stock not found with VIN: {}", dto.getVinNumber());
                        return new IllegalArgumentException("MDDP stock not found with VIN: " + dto.getVinNumber());
                    });

            // Update fields from DTO (based on screenshot fields and MDDP-specific fields)
            if (dto.getVehicleModelId() != null) {
                existingStock.setVehicleModelId(getVehicleModelEntity(dto.getVehicleModelId()));
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
            if (dto.getVariant() != null) { // Grade is mapped to Variant in the DTO
                existingStock.setVariant(dto.getVariant());
            }
            if (dto.getQuantity() != null) {
                existingStock.setQuantity(dto.getQuantity());
            }
            if (dto.getSuffix() != null) {
                existingStock.setSuffix(dto.getSuffix());
            }
            if (dto.getColour() != null) { // Color
                existingStock.setColour(dto.getColour());
            }
            if (dto.getEngineColour() != null) { // Exterior Color (E.Color)
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

            // Update audit fields
            existingStock.setUpdatedAt(LocalDateTime.now());
            existingStock.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");

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
    public ManufacturerOrder saveManufacturerOrder(ManufacturerOrderDTO dto) {
        log.info("Starting insertion of single manufacturer order at {}", LocalDateTime.now());

        if (dto == null) {
            log.error("Received null DTO");
            throw new IllegalArgumentException("Manufacturer order DTO cannot be null");
        }

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

        ManufacturerOrder order = new ManufacturerOrder();
        order.setVehicleVariantId(getVehicleVariantEntity(dto.getVehicleVariantId()));
        order.setManufacturerLocation(dto.getManufacturerLocation());
        order.setOrderStatus(OrderStatus.valueOf(dto.getOrderStatus()));
        order.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
        order.setCreatedBy(dto.getCreatedBy() != null ? dto.getCreatedBy() : "system");
        order.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : "system");

        try {
            ManufacturerOrder savedOrder = manufacturerOrderRepository.save(order);
            log.info("Successfully saved manufacturer order with ID: {}", savedOrder.getManufacturerId());
            return savedOrder;
        } catch (Exception e) {
            log.error("Failed to save manufacturer order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save manufacturer order: " + e.getMessage(), e);
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
}