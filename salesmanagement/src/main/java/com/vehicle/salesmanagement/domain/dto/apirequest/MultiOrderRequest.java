package com.vehicle.salesmanagement.domain.dto.apirequest;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MultiOrderRequest {
    @NotNull(message = "Vehicle orders list cannot be null")
    @Size(min = 1, message = "At least one vehicle order is required")
    private List<@Valid OrderRequest> vehicleOrders;
}