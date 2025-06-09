package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DispatchRequest {

    @NotNull(message = "Customer order ID cannot be null")
    //@Pattern(regexp = "^TYT-\\d{4}-\\d{3}$", message = "Customer order ID must be in the format TYT-YYYY-NNN")
  //  @Size(min = 11, max = 11, message = "Customer order ID must be exactly 11 characters")
    private String customerOrderId;

    @NotNull(message = "Dispatched by cannot be null")
    @Size(min = 1, max = 100, message = "Dispatched by must be between 1 and 100 characters")
    private String dispatchedBy;
//    private String createdBy;
//    private String updatedBy;
}