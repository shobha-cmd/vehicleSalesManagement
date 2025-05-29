package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleOrderGridDTO {
    private Long customerOrderId;
    private String customerName;
    private String modelName;
    private Integer quantity;
    private String variant;
    private OrderStatus orderStatus;
}