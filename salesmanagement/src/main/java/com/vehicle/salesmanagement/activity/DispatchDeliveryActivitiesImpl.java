package com.vehicle.salesmanagement.activity;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.service.DispatchDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchDeliveryActivitiesImpl implements DispatchDeliveryActivities {

    private final DispatchDeliveryService dispatchDeliveryService;
    private final VehicleOrderDetailsRepository vehicleOrderDetailsRepository;

    @Override
    public DispatchResponse initiateDispatch(DispatchRequest dispatchRequest) {
        log.info("Activity: Initiating dispatch for order ID: {}", dispatchRequest.getCustomerOrderId());
        return dispatchDeliveryService.initiateDispatch(dispatchRequest);
    }

    @Override
    public DeliveryResponse confirmDelivery(DeliveryRequest deliveryRequest) {
        log.info("Activity: Confirming delivery for order ID: {}", deliveryRequest.getCustomerOrderId());
        return dispatchDeliveryService.confirmDelivery(deliveryRequest);
    }

    @Override
    public Optional<VehicleOrderDetails> getVehicleOrderDetails(Long orderId) {
        log.info("Activity: Fetching vehicle order details for order ID: {}", orderId);
        return vehicleOrderDetailsRepository.findById(orderId);
    }
}