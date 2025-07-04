package com.vehicle.salesmanagement.domain.entity.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_details_history", schema = "sales_tracking")
public class StockDetailsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private StockDetails stockId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @ManyToOne
    @JoinColumn(name = "vehicle_model_id")
    private VehicleModel vehicleModelId;

    @ManyToOne
    @JoinColumn(name = "vehicle_variant_id")
    private VehicleVariant vehicleVariantId;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "colour")
    private String colour;

    @Column(name = "modelName")
    private String modelName;

    @Column(name = "engine_colour")
    private String engineColour;

    @Column(name = "transmission_type")
    private String transmissionType;

    @Column(name = "variant")
    private String variant;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "stock_status")
    private String stockStatus;

    @Column(name = "interior_colour")
    private String interiorColour;

    @Column(name = "stock_arrival_date")
    private String stockArrivalDate;

    @Column(name = "stock_history")
    private String stockHistory;

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public StockDetails getStockId() {
        return stockId;
    }

    public void setStockId(StockDetails stockDetails) {
        this.stockId = stockDetails;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
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

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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

    public String getStockArrivalDate() {
        return stockArrivalDate;
    }

    public void setStockArrivalDate(String stockArrivalDate) {
        this.stockArrivalDate = stockArrivalDate;
    }

    public String getStockHistory() {
        return stockHistory;
    }

    public void setStockHistory(String stockHistory) {
        this.stockHistory = stockHistory;
    }
}