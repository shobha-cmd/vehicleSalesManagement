package com.vehicle.salesmanagement.domain.dto.apirequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryRequest {

    @NotNull(message = "Customer order ID cannot be null")
   // @Pattern(regexp = "^TYT-\\d{4}-\\d{3}$", message = "Customer order ID must be in the format TYT-YYYY-NNN")
   // @Size(min = 11, max = 11, message = "Customer order ID must be exactly 11 characters")
    private String customerOrderId;

    @NotNull(message = "Delivered by cannot be null")
    @Size(min = 1, max = 100, message = "Delivered by must be between 1 and 100 characters")
    private String deliveredBy;

    @NotNull(message = "Recipient name cannot be null")
    @Size(min = 1, max = 100, message = "Recipient name must be between 1 and 100 characters")
    private String recipientName;
//    private String createdBy;
//    private String updatedBy;
}