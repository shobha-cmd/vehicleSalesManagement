package com.vehicle.salesmanagement.errorhandling;

class FinanceDetailsExistException extends RuntimeException {
    private final Long orderId;

    public FinanceDetailsExistException(Long orderId) {
        super(String.format("Finance details already exist for order ID: %d", orderId));
        this.orderId = orderId;
    }

    public FinanceDetailsExistException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}