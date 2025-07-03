package com.vehicle.salesmanagement.domain.dto.apiresponse;

import com.vehicle.salesmanagement.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleOrderGridDTO {
    private String customerOrderId;
    private String customerName;
    private String modelName;
    private Integer quantity;
    private String variant;
    private OrderStatus orderStatus;
    private LocalDate expectedDeliveryDate;
}