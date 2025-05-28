package com.vehicle.salesmanagement.domain.dto.apiresponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAttributesResponse {
    private List<String> modelNames;
    private Map<String, ModelAttributes> modelDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelAttributes {
        private List<String> variants;
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
    }
}