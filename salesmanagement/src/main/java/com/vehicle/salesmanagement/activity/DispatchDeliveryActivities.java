package com.vehicle.salesmanagement.activity;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import io.temporal.activity.ActivityInterface;

import java.util.Optional;

@ActivityInterface
public interface DispatchDeliveryActivities {
    DispatchResponse initiateDispatch(DispatchRequest request);
    DeliveryResponse confirmDelivery(DeliveryRequest request);
    Optional<VehicleOrderDetails> getVehicleOrderDetails(Long orderId);
}