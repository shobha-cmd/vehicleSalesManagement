package com.vehicle.salesmanagement.domain.dto.apirequest;

import java.time.LocalDateTime;

public class ManufacturerOrderDTO {
    private Long vehicleVariantId;
    private Long ManufacturerId;
    private String manufacturerLocation;
    private String orderStatus;
    private LocalDateTime estimatedArrivalDate;
    private String modelName;
    private String fuelType;
    private String colour;
    private String variant;
    private String vinNumber;
    private String suffix;
    private String interiorColour;
    private String engineColour;
    private String transmissionType;

    public String getTransmissionType() {
        return transmissionType;
    }

    public void setTransmissionType(String transmissionType) {
        this.transmissionType = transmissionType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getVinNumber() {
        return vinNumber;
    }

    public void setVinNumber(String vinNumber) {
        this.vinNumber = vinNumber;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getInteriorColour() {
        return interiorColour;
    }

    public void setInteriorColour(String interiorColour) {
        this.interiorColour = interiorColour;
    }

    public String getEngineColour() {
        return engineColour;
    }

    public void setEngineColour(String engineColour) {
        this.engineColour = engineColour;
    }

    public Long getVehicleVariantId() {
        return vehicleVariantId;
    }

    public void setVehicleVariantId(Long vehicleVariantId) {
        this.vehicleVariantId = vehicleVariantId;
    }

    public String getManufacturerLocation() {
        return manufacturerLocation;
    }

    public void setManufacturerLocation(String manufacturerLocation) {
        this.manufacturerLocation = manufacturerLocation;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getEstimatedArrivalDate() {
        return estimatedArrivalDate;
    }

    public void setEstimatedArrivalDate(LocalDateTime estimatedArrivalDate) {
        this.estimatedArrivalDate = estimatedArrivalDate;
    }

    public Long getManufacturerId() {
        return ManufacturerId;
    }

    public void setManufacturerId(Long manufacturerId) {
        ManufacturerId = manufacturerId;
    }

}