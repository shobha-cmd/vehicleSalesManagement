package com.vehicle.salesmanagement.domain.dto.apiresponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class VehicleAttributesResponse {
    private List<String> modelNames;
    private Map<String, ModelAttributes> modelDetails;

    public static class ModelAttributes {
        private Long vehicleModelId;
        private List<Variant> variants;
        private List<String> colours;
        private List<String> engineColours;
        private List<String> interiorColours;
        private List<String> fuelTypes;
        private List<String> transmissionTypes;
        private List<BigDecimal> prices;
        private List<Integer> yearsOfManufacture;
        private List<String> bodyTypes;
        private List<Double> fuelTankCapacities;
        private List<Integer> numberOfAirbags;
        private List<Double> mileageCities;
        private List<Double> mileageHighways;
        private List<Integer> seatingCapacities;
        private List<String> maxPowers;
        private List<String> maxTorques;
        private List<Integer> topSpeeds;
        private List<Integer> wheelBases;
        private List<Integer> widths;
        private List<Integer> lengths;
        private List<String> safetyFeatures;
        private List<String> infotainments;
        private List<String> comforts;
        private List<String> suffixes;
        private List<String> engineCapacities;

        public Long getVehicleModelId() {
            return vehicleModelId;
        }

        public void setVehicleModelId(Long vehicleModelId) {
            this.vehicleModelId = vehicleModelId;
        }

        public List<Variant> getVariants() {
            return variants;
        }

        public void setVariants(List<Variant> variants) {
            this.variants = variants;
        }

        public List<String> getColours() {
            return colours;
        }

        public void setColours(List<String> colours) {
            this.colours = colours;
        }

        public List<String> getEngineColours() {
            return engineColours;
        }

        public void setEngineColours(List<String> engineColours) {
            this.engineColours = engineColours;
        }

        public List<String> getInteriorColours() {
            return interiorColours;
        }

        public void setInteriorColours(List<String> interiorColours) {
            this.interiorColours = interiorColours;
        }

        public List<String> getFuelTypes() {
            return fuelTypes;
        }

        public void setFuelTypes(List<String> fuelTypes) {
            this.fuelTypes = fuelTypes;
        }

        public List<String> getTransmissionTypes() {
            return transmissionTypes;
        }

        public void setTransmissionTypes(List<String> transmissionTypes) {
            this.transmissionTypes = transmissionTypes;
        }

        public List<BigDecimal> getPrices() {
            return prices;
        }

        public void setPrices(List<BigDecimal> prices) {
            this.prices = prices;
        }

        public List<Integer> getYearsOfManufacture() {
            return yearsOfManufacture;
        }

        public void setYearsOfManufacture(List<Integer> yearsOfManufacture) {
            this.yearsOfManufacture = yearsOfManufacture;
        }

        public List<String> getBodyTypes() {
            return bodyTypes;
        }

        public void setBodyTypes(List<String> bodyTypes) {
            this.bodyTypes = bodyTypes;
        }

        public List<Double> getFuelTankCapacities() {
            return fuelTankCapacities;
        }

        public void setFuelTankCapacities(List<Double> fuelTankCapacities) {
            this.fuelTankCapacities = fuelTankCapacities;
        }

        public List<Integer> getNumberOfAirbags() {
            return numberOfAirbags;
        }

        public void setNumberOfAirbags(List<Integer> numberOfAirbags) {
            this.numberOfAirbags = numberOfAirbags;
        }

        public List<Double> getMileageCities() {
            return mileageCities;
        }

        public void setMileageCities(List<Double> mileageCities) {
            this.mileageCities = mileageCities;
        }

        public List<Double> getMileageHighways() {
            return mileageHighways;
        }

        public void setMileageHighways(List<Double> mileageHighways) {
            this.mileageHighways = mileageHighways;
        }

        public List<Integer> getSeatingCapacities() {
            return seatingCapacities;
        }

        public void setSeatingCapacities(List<Integer> seatingCapacities) {
            this.seatingCapacities = seatingCapacities;
        }

        public List<String> getMaxPowers() {
            return maxPowers;
        }

        public void setMaxPowers(List<String> maxPowers) {
            this.maxPowers = maxPowers;
        }

        public List<String> getMaxTorques() {
            return maxTorques;
        }

        public void setMaxTorques(List<String> maxTorques) {
            this.maxTorques = maxTorques;
        }

        public List<Integer> getTopSpeeds() {
            return topSpeeds;
        }

        public void setTopSpeeds(List<Integer> topSpeeds) {
            this.topSpeeds = topSpeeds;
        }

        public List<Integer> getWheelBases() {
            return wheelBases;
        }

        public void setWheelBases(List<Integer> wheelBases) {
            this.wheelBases = wheelBases;
        }

        public List<Integer> getWidths() {
            return widths;
        }

        public void setWidths(List<Integer> widths) {
            this.widths = widths;
        }

        public List<Integer> getLengths() {
            return lengths;
        }

        public void setLengths(List<Integer> lengths) {
            this.lengths = lengths;
        }

        public List<String> getSafetyFeatures() {
            return safetyFeatures;
        }

        public void setSafetyFeatures(List<String> safetyFeatures) {
            this.safetyFeatures = safetyFeatures;
        }

        public List<String> getInfotainments() {
            return infotainments;
        }

        public void setInfotainments(List<String> infotainments) {
            this.infotainments = infotainments;
        }

        public List<String> getComforts() {
            return comforts;
        }

        public void setComforts(List<String> comforts) {
            this.comforts = comforts;
        }

        public List<String> getSuffixes() {
            return suffixes;
        }

        public void setSuffixes(List<String> suffixes) {
            this.suffixes = suffixes;
        }

        public List<String> getEngineCapacities() {
            return engineCapacities;
        }

        public void setEngineCapacities(List<String> engineCapacities) {
            this.engineCapacities = engineCapacities;
        }

        // Getters and setters
    }

    public static class Variant {
        private String name;
        private Long vehicleVariantId;

        public Variant(String name, Long vehicleVariantId) {
            this.name = name;
            this.vehicleVariantId = vehicleVariantId;
        }

        // Getters and setters
        public String getName() { return name; }
        public Long getVehicleVariantId() { return vehicleVariantId; }
    }

    public List<String> getModelNames() {
        return modelNames;
    }

    public void setModelNames(List<String> modelNames) {
        this.modelNames = modelNames;
    }

    public Map<String, ModelAttributes> getModelDetails() {
        return modelDetails;
    }

    public void setModelDetails(Map<String, ModelAttributes> modelDetails) {
        this.modelDetails = modelDetails;
    }
// Getters and setters
}