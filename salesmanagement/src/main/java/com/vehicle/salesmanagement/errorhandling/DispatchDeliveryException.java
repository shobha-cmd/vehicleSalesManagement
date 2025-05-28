package com.vehicle.salesmanagement.errorhandling;

class DispatchDeliveryException extends RuntimeException {
    private final Long orderId;
    private final String type;

    public DispatchDeliveryException(Long orderId, String type) {
        super(String.format("%s details already exist for order ID: %d", type, orderId));
        this.orderId = orderId;
        this.type = type;
    }

    public DispatchDeliveryException(Long orderId, String type, String message) {
        super(message);
        this.orderId = orderId;
        this.type = type;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getType() {
        return type;
    }
}