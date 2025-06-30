package com.vehicle.salesmanagement.domain.dto.apirequest;

public class StockDetailsDTO {

    private Long vehicleModelId;

    private Long vehicleVariantId;

    private String suffix;

    private String fuelType;

    private String colour;

    private String engineColour;

    private String transmissionType;

    private String variant;

    private Integer quantity;

    private String stockStatus;

    private String interiorColour;

    private String vinNumber;
    private String modelName;

    public String getModelName() {

        return modelName;

    }
    public void setModelName(String modelName) {

        this.modelName = modelName;

    }
    public Long getVehicleModelId() {

        return vehicleModelId;

    }
    public void setVehicleModelId(Long vehicleModelId) {

        this.vehicleModelId = vehicleModelId;

    }
    public Long getVehicleVariantId() {

        return vehicleVariantId;

    }
    public void setVehicleVariantId(Long vehicleVariantId) {

        this.vehicleVariantId = vehicleVariantId;

    }
    public String getSuffix() {

        return suffix;

    }
    public void setSuffix(String suffix) {

        this.suffix = suffix;

    }
    public String getFuelType() {

        return fuelType;

    }
    public void setFuelType(String fuelType) {

        this.fuelType = fuelType;
    }
    public String getColour() {

        return colour;
    }
    public void setColour(String colour) {

        this.colour = colour;

    }
    public String getEngineColour() {

        return engineColour;

    }
    public void setEngineColour(String engineColour) {

        this.engineColour = engineColour;

    }
    public String getTransmissionType() {

        return transmissionType;

    }
    public void setTransmissionType(String transmissionType) {

        this.transmissionType = transmissionType;

    }
    public String getVariant() {

        return variant;

    }
    public void setVariant(String variant) {

        this.variant = variant;

    }
    public Integer getQuantity() {

        return quantity;

    }
    public void setQuantity(Integer quantity) {

        this.quantity = quantity;

    }
    public String getStockStatus() {

        return stockStatus;

    }
    public void setStockStatus(String stockStatus) {

        this.stockStatus = stockStatus;

    }
    public String getInteriorColour() {

        return interiorColour;

    }
    public void setInteriorColour(String interiorColour) {

        this.interiorColour = interiorColour;
    }
    public String getVinNumber() {

        return vinNumber;
    }
    public void setVinNumber(String vinNumber) {
        this.vinNumber = vinNumber;
    }
    public void setStockId(Long stockId) {

    }
}