package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class VehicleModelDTO {

    private String modelName;
    private String createdBy;
    private String updatedBy;
}