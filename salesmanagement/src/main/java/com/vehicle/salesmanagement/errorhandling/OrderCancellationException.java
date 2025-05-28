package com.vehicle.salesmanagement.errorhandling;

import com.vehicle.salesmanagement.enums.OrderStatus;

public class OrderCancellationException extends RuntimeException {
    private final Long orderId;
    private final OrderStatus currentStatus;

    public OrderCancellationException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = null;
    }

    public OrderCancellationException(Long orderId, OrderStatus currentStatus, String message) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}