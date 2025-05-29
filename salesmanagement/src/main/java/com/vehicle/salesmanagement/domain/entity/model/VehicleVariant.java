package com.vehicle.salesmanagement.domain.entity.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicle_variant", schema = "sales_tracking")
public class VehicleVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicleVariant_Id", nullable = false)
    private Long vehicleVariantId;

    @ManyToOne
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    private VehicleModel vehicleModelId;

    @Column(name = "variant", length = 100, nullable = false)
    private String variant;

    @Column(name = "suffix", length = 50)
    private String suffix;

    @Column(name = "safety_feature", length = 500)
    private String safetyFeature;

    @Column(name = "engine_colour", length = 50)
    private String engineColour;

    @Column(name = "colour", length = 255) // Increased from 50 to 255
    private String colour;

    @Column(name = "interior_colour", length = 100) // Increased from 50 to 100
    private String interiorColour;

    @Column(name = "transmission_type", length = 100) // Increased from 50 to 100
    private String transmissionType;


    @Column(name = "vin_number", length = 50, unique = true)
    private String vinNumber;

    @Column(name = "engine_capacity", length = 50)
    private String engineCapacity;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "year_of_manufacture")
    private Integer yearOfManufacture;

    @Column(name = "body_type", length = 100)
    private String bodyType;

    @Column(name = "fuel_tank_capacity", precision = 5, scale = 2)
    private BigDecimal fuelTankCapacity;

    @Column(name = "seating_capacity")
    private Integer seatingCapacity;

    @Column(name = "max_power", length = 50)
    private String maxPower;

    @Column(name = "max_torque", length = 50)
    private String maxTorque;

    @Column(name = "top_speed", length = 50)
    private String topSpeed;

    @Column(name = "wheel_base", length = 50)
    private String wheelBase;

    @Column(name = "width", length = 50)
    private String width;

    @Column(name = "length", length = 50)
    private String length;

    @Column(name = "infotainment", length = 100)
    private String infotainment;

    @Column(name = "comfort", length = 255)
    private String comfort;

    @Column(name = "number_of_airbags")
    private Integer numberOfAirBags;

    @Column(name = "mileage_city", precision = 5, scale = 2)
    private BigDecimal mileageCity;

    @Column(name = "mileage_highway", precision = 5, scale = 2)
    private BigDecimal mileageHighway;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public Long getVehicleVariantId() {
        return vehicleVariantId;
    }

    public void setVehicleVariantId(Long vehicleVariantId) {
        this.vehicleVariantId = vehicleVariantId;
    }

    public VehicleModel getVehicleModelId() {
        return vehicleModelId;
    }

    public void setVehicleModelId(VehicleModel vehicleModelId) {
        this.vehicleModelId = vehicleModelId;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getSafetyFeature() {
        return safetyFeature;
    }

    public void setSafetyFeature(String safetyFeature) {
        this.safetyFeature = safetyFeature;
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

    public String getEngineCapacity() {
        return engineCapacity;
    }

    public void setEngineCapacity(String engineCapacity) {
        this.engineCapacity = engineCapacity;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getYearOfManufacture() {
        return yearOfManufacture;
    }

    public void setYearOfManufacture(Integer yearOfManufacture) {
        this.yearOfManufacture = yearOfManufacture;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public BigDecimal getFuelTankCapacity() {
        return fuelTankCapacity;
    }

    public void setFuelTankCapacity(BigDecimal fuelTankCapacity) {
        this.fuelTankCapacity = fuelTankCapacity;
    }

    public Integer getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(Integer seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public String getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(String maxPower) {
        this.maxPower = maxPower;
    }

    public String getMaxTorque() {
        return maxTorque;
    }

    public void setMaxTorque(String maxTorque) {
        this.maxTorque = maxTorque;
    }

    public String getTopSpeed() {
        return topSpeed;
    }

    public void setTopSpeed(String topSpeed) {
        this.topSpeed = topSpeed;
    }

    public String getWheelBase() {
        return wheelBase;
    }

    public void setWheelBase(String wheelBase) {
        this.wheelBase = wheelBase;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getInfotainment() {
        return infotainment;
    }

    public void setInfotainment(String infotainment) {
        this.infotainment = infotainment;
    }

    public String getComfort() {
        return comfort;
    }

    public void setComfort(String comfort) {
        this.comfort = comfort;
    }

    public Integer getNumberOfAirBags() {
        return numberOfAirBags;
    }

    public void setNumberOfAirBags(Integer numberOfAirBags) {
        this.numberOfAirBags = numberOfAirBags;
    }

    public BigDecimal getMileageCity() {
        return mileageCity;
    }

    public void setMileageCity(BigDecimal mileageCity) {
        this.mileageCity = mileageCity;
    }

    public BigDecimal getMileageHighway() {
        return mileageHighway;
    }

    public void setMileageHighway(BigDecimal mileageHighway) {
        this.mileageHighway = mileageHighway;
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