package com.vehicle.salesmanagement.domain.entity.model;


import com.vehicle.salesmanagement.enums.StockStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mddp_stock", schema = "sales_tracking")
@AllArgsConstructor
@NoArgsConstructor
public class MddpStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mddp_Id", nullable = false)
    private Integer mddpId;

    @Column(name = "expected_dispatch_date", nullable = false)
    private LocalDateTime expectedDispatchDate;

    @Column(name = "expected_delivery_date", nullable = false)
    private LocalDateTime expectedDeliveryDate;

    @ManyToOne
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    private VehicleModel vehicleModelId;

    @ManyToOne
    @JoinColumn(name = "vehicle_variant_id", nullable = false)
    private VehicleVariant vehicleVariantId;

    @Column(name="model_name",length=50)
    private String modelName;


    @Column(name = "suffix", length = 50)
    private String suffix;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Column(name = "colour", length = 50)
    private String colour;

    @Column(name = "engine_colour", length = 50)
    private String engineColour;

    @Column(name = "transmission_type", length = 50)
    private String transmissionType;

    @Column(name = "variant", length = 50)
    private String variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus; // Block/Reserve

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "interior_colour", length = 50)
    private String interiorColour;

    @Column(name = "vin_Number", length = 50, unique = true, nullable = false)
    private String vinNumber;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public Integer getMddpId() {
        return mddpId;
    }

    public void setMddpId(Integer mddpId) {
        this.mddpId = mddpId;
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

    public VehicleModel getVehicleModelId() {
        return vehicleModelId;
    }

    public void setVehicleModelId(VehicleModel vehicleModelId) {
        this.vehicleModelId = vehicleModelId;
    }

    public VehicleVariant getVehicleVariantId() {
        return vehicleVariantId;
    }

    public void setVehicleVariantId(VehicleVariant vehicleVariantId) {
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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

    public StockStatus getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(StockStatus stockStatus) {
        this.stockStatus = stockStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}