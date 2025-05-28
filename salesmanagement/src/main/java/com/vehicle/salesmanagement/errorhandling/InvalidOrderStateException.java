package com.vehicle.salesmanagement.errorhandling;

import com.vehicle.salesmanagement.enums.OrderStatus;

class InvalidOrderStateException extends RuntimeException {
    private final OrderStatus currentStatus;
    private final OrderStatus expectedStatus;

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus expectedStatus) {
        super(String.format("Invalid order state: expected %s, found %s", expectedStatus, currentStatus));
        this.currentStatus = currentStatus;
        this.expectedStatus = expectedStatus;
    }

    public InvalidOrderStateException(OrderStatus currentStatus, OrderStatus expectedStatus, String message) {
        super(message);
        this.currentStatus = currentStatus;
        this.expectedStatus = expectedStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getExpectedStatus() {
        return expectedStatus;
    }
}