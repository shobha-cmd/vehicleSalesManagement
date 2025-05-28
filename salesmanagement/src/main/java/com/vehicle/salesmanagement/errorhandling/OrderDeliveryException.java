package com.vehicle.salesmanagement.errorhandling;

import com.vehicle.salesmanagement.enums.OrderStatus;

class OrderDeliveryException extends RuntimeException {
    private final Long orderId;
    private final OrderStatus currentStatus;
    private final OrderStatus expectedStatus;

    public OrderDeliveryException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = null;
        this.expectedStatus = null;
    }

    public OrderDeliveryException(Long orderId, OrderStatus currentStatus, OrderStatus expectedStatus) {
        super(String.format("Invalid order state for delivery: expected %s, found %s", expectedStatus, currentStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.expectedStatus = expectedStatus;
    }

    public OrderDeliveryException(Long orderId, OrderStatus currentStatus, OrderStatus expectedStatus, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.expectedStatus = expectedStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getExpectedStatus() {
        return expectedStatus;
    }
}