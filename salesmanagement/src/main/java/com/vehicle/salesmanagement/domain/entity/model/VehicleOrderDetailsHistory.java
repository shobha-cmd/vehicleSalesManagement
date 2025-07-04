package com.vehicle.salesmanagement.domain.entity.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_order_details_history", schema = "sales_tracking")
public class VehicleOrderDetailsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne
    @JoinColumn(name = "customer_order_id", nullable = false)
    private VehicleOrderDetails vehicleOrderDetailsId;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "aadhar_no")
    private String aadharNo;

    @Column(name = "colour")
    private String colour;

    @Column(name = "current_address")
    private String currentAddress;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "email")
    private String email;

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "pan_no")
    private String panNo;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "permanent_address")
    private String permanentAddress;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "transmission_type")
    private String transmissionType;

    @Column(name = "variant")
    private String variant;

    @ManyToOne
    @JoinColumn(name = "vehicle_model_id")
    private VehicleModel vehicleModelId;

    @ManyToOne
    @JoinColumn(name = "vehicle_variant_id")
    private VehicleVariant vehicleVariantId;

    @Column(name = "order_status_history")
    private String orderStatusHistory;

    @Column(name = "expected_delivery_date")
    private String expectedDeliveryDate;

    // Getters and Setters

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public VehicleOrderDetails getVehicleOrderDetailsId() {
        return vehicleOrderDetailsId;
    }

    public void setVehicleOrderDetailsId(VehicleOrderDetails vehicleOrderDetailsId) {
        this.vehicleOrderDetailsId = vehicleOrderDetailsId;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getAadharNo() {
        return aadharNo;
    }

    public void setAadharNo(String aadharNo) {
        this.aadharNo = aadharNo;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public void setCurrentAddress(String currentAddress) {
        this.currentAddress = currentAddress;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPanNo() {
        return panNo;
    }

    public void setPanNo(String panNo) {
        this.panNo = panNo;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public String getOrderStatusHistory() {
        return orderStatusHistory;
    }

    public void setOrderStatusHistory(String orderStatusHistory) {
        this.orderStatusHistory = orderStatusHistory;
    }

    public String getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(String expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public void setCustomerOrderId(String customerOrderId) {
        // No implementation needed as customerOrderId is managed via vehicleOrderDetailsId
    }
}