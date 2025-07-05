package com.vehicle.salesmanagement.domain.dto.apirequest;

import java.time.LocalDateTime;

public class MddpStockDTO {
    private Long vehicleModelId;
    private Long vehicleVariantId;
    private Long mddpId;
    private String modelName;
    private String suffix;
    private String fuelType;
    private String colour;
    private String engineColour;
    private String transmissionType;
    private String variant;
    private Integer quantity;
    private String stockStatus;
    private String interiorColour;
    private LocalDateTime expectedDispatchDate;
    private LocalDateTime expectedDeliveryDate;
    private String stockArrivalDate;

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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getSuffix() {
        return suffix;
    }

    public Long getMddpId() {
        return mddpId;
    }

    public void setMddpId(Long mddpId) {
        this.mddpId = mddpId;
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

    public LocalDateTime getExpectedDispatchDate() {
        return expectedDispatchDate;
    }

    public void setExpectedDispatchDate(LocalDateTime expectedDispatchDate) {
        this.expectedDispatchDate = expectedDispatchDate;
    }

    public LocalDateTime getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDateTime expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getStockArrivalDate() {
        return stockArrivalDate;
    }

    public void setStockArrivalDate(String stockArrivalDate) {
        this.stockArrivalDate = stockArrivalDate;
    }


}