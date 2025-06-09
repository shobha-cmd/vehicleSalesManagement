package com.vehicle.salesmanagement.controller;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.service.DispatchDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
@Tag(name = "Dispatch and Delivery Management")
public class DispatchDeliveryController {

    private final @Qualifier("dispatchDeliveryWorkflowClient") WorkflowClient workflowClient;
    private final DispatchDeliveryService dispatchDeliveryService;

    @PostMapping("/initiateDispatch")
    @Operation(summary = "Initiate dispatch process", description = "Signals the parent workflow to initiate the dispatch process for a vehicle order")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Dispatch process initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<DispatchResponse> initiateDispatch(@Valid @RequestBody DispatchRequest dispatchRequest) {
        log.info("Initiating dispatch process for order ID: {}", dispatchRequest.getCustomerOrderId());

        DispatchResponse dispatchResponse;
        try {
            dispatchResponse = dispatchDeliveryService.initiateDispatch(dispatchRequest);
        } catch (Exception e) {
            log.error("Failed to initiate dispatch for order ID: {}: {}", dispatchRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to initiate dispatch: " + e.getMessage(),
                    null
            );
        }

        // Signal the parent workflow to initiate dispatch
        String parentWorkflowId = "parent-" + dispatchRequest.getCustomerOrderId();
        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(parentWorkflowId);
            workflow.signal("initiateDispatch", dispatchRequest);
            log.info("Dispatch initiation signal sent to parent workflow for order ID: {}", dispatchRequest.getCustomerOrderId());
        } catch (WorkflowNotFoundException e) {
            log.error("Parent workflow not found for order ID: {}: {}", dispatchRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Parent workflow not found for workflowId='" + parentWorkflowId + "'",
                    null
            );
        } catch (Exception e) {
            log.error("Failed to signal parent workflow for order ID: {}: {}", dispatchRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to signal parent workflow to initiate dispatch: " + e.getMessage(),
                    dispatchResponse
            );
        }

        log.info("Dispatch process initiated, vehicle order details updated to DISPATCHED for order ID: {}", dispatchRequest.getCustomerOrderId());
        return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                "Dispatch process initiated for order ID: " + dispatchRequest.getCustomerOrderId(),
                dispatchResponse
        );
    }

    @PostMapping("/confirmDelivery")
    @Operation(summary = "Confirm vehicle delivery", description = "Signals the parent workflow to confirm delivery and updates order status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery confirmed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<DeliveryResponse> confirmDelivery(@Valid @RequestBody DeliveryRequest deliveryRequest) {
        log.info("Confirming delivery for order ID: {}", deliveryRequest.getCustomerOrderId());

        DeliveryResponse deliveryResponse;
        try {
            deliveryResponse = dispatchDeliveryService.confirmDelivery(deliveryRequest);
        } catch (IllegalStateException e) {
            log.error("Failed to confirm delivery for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to confirm delivery: " + e.getMessage(),
                    null
            );
        } catch (Exception e) {
            log.error("Failed to confirm delivery for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to confirm delivery: " + e.getMessage(),
                    null
            );
        }

        // Signal the parent workflow to confirm delivery
        String parentWorkflowId = "parent-" + deliveryRequest.getCustomerOrderId();
        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(parentWorkflowId);
            workflow.signal("confirmDelivery", deliveryRequest);
            log.info("Delivery confirmation signal sent to parent workflow for order ID: {}", deliveryRequest.getCustomerOrderId());
        } catch (WorkflowNotFoundException e) {
            log.warn("Parent workflow not found for order ID: {}: workflowId='{}', attempting to query workflow history",
                    deliveryRequest.getCustomerOrderId(), parentWorkflowId);
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Delivery confirmed, but parent workflow not found for workflowId='" + parentWorkflowId + "'. Check workflow history for details.",
                    deliveryResponse
            );
        } catch (Exception e) {
            log.error("Failed to signal parent workflow for delivery confirmation for order ID: {}: {}", deliveryRequest.getCustomerOrderId(), e.getMessage());
            return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Delivery confirmed, but failed to signal parent workflow: " + e.getMessage(),
                    deliveryResponse
            );
        }

        log.info("Delivery confirmed, vehicle order details updated to DELIVERED for order ID: {}", deliveryRequest.getCustomerOrderId());
        return new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse<>(
                HttpStatus.OK.value(),
                "Delivery confirmed successfully",
                deliveryResponse
        );
    }
}