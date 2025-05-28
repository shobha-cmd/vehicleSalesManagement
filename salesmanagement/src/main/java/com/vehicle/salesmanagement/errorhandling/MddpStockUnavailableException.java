package com.vehicle.salesmanagement.errorhandling;

public class MddpStockUnavailableException extends RuntimeException {
    public MddpStockUnavailableException(String message) {
        super(message);
    }
}