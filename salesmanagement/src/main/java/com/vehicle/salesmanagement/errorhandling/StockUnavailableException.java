package com.vehicle.salesmanagement.errorhandling;

class StockUnavailableException extends RuntimeException {
    private final Long variantId;

    public StockUnavailableException(Long variantId) {
        super(String.format("No available stock for variant ID: %d", variantId));
        this.variantId = variantId;
    }

    public StockUnavailableException(Long variantId, String message) {
        super(message);
        this.variantId = variantId;
    }

    public Long getVariantId() {
        return variantId;
    }
}