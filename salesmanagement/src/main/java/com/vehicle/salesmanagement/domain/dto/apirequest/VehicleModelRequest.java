package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehicleModelRequest {

    @NotBlank(message = "Model name cannot be blank")
    @Size(min = 1, max = 100, message = "Model name must be between 1 and 100 characters")
    private String modelName;

}
