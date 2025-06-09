package com.vehicle.salesmanagement.activity;

import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface VehicleOrderActivities {
    void saveOrderDetails(VehicleOrderDetails vehicleOrderDetails);
    OrderResponse checkStockAvailability(OrderRequest orderRequest);
    OrderResponse confirmOrder(OrderResponse orderResponse);
    OrderResponse cancelOrder(String customerOrderId);
}