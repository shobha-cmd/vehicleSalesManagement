package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.activity.VehicleOrderActivities;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.enums.OrderStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
public class VehicleOrderWorkflowImpl implements VehicleOrderWorkflow {

    private final VehicleOrderActivities activities;
    private boolean isCanceled = false;

    public VehicleOrderWorkflowImpl() {
        ActivityOptions options = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build();
        this.activities = Workflow.newActivityStub(VehicleOrderActivities.class, options);
    }

    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        log.info("Workflow started for customer: {}", orderRequest.getCustomerName());
        OrderResponse response = null;
        try {
            response = activities.checkStockAvailability(orderRequest);

            if (isCanceled) {
                log.info("Order canceled during workflow for customer: {}", orderRequest.getCustomerName());
                return activities.cancelOrder(Long.valueOf(Workflow.getInfo().getWorkflowId().split("-")[1]));
            }

            // Respect the status returned by checkStockAvailability
            if (response.getOrderStatus() == OrderStatus.BLOCKED) {
                log.info("Stock blocked for customer: {}", orderRequest.getCustomerName());
                return response;
            } else if (response.getOrderStatus() == OrderStatus.PENDING) {
                log.info("Stock not available, manufacturer order placed for: {}", orderRequest.getCustomerName());
                return response; // Keep the PENDING status as set by placeManufacturerOrder
            } else if (response.getOrderStatus() == OrderStatus.COMPLETED) {
                log.info("Order confirmed for customer: {}", orderRequest.getCustomerName());
                return activities.confirmOrder(response);
            }

            // Fallback for unexpected status
            log.warn("Unexpected status for customer: {}. Defaulting to PENDING.", orderRequest.getCustomerName());
            response = mapToOrderResponse(orderRequest);
            response.setOrderStatus(OrderStatus.PENDING);
            return response;
        } catch (Exception e) {
            log.error("Workflow failed for customer {}: {}", orderRequest.getCustomerName(), e.getMessage(), e);
            response = mapToOrderResponse(orderRequest);
            response.setOrderStatus(OrderStatus.PENDING);
            return response;
        } finally {
            if (response != null && response.getOrderStatus() == OrderStatus.PENDING) {
                log.info("Starting 24-hour wait for manufacturer order for customer: {}", orderRequest.getCustomerName());
                Workflow.await(Duration.ofHours(24), () -> isCanceled);
                if (isCanceled) {
                    log.info("Order canceled during manufacturer wait for customer: {}", orderRequest.getCustomerName());
                    String workflowId = Workflow.getInfo().getWorkflowId();
                    Long customerOrderId = Long.valueOf(workflowId.split("-")[1]); // Changed to customerOrderId
                    activities.cancelOrder(customerOrderId); // Changed to customerOrderId
                } else {
                    log.info("Manufacturer order wait completed, could update status to PROCESSING or COMPLETED if needed");
                }
            }
        }
    }

    private OrderResponse mapToOrderResponse(OrderRequest request) {
        if (request.getCustomerOrderId() == null) {
            log.error("CustomerOrderId is null in OrderRequest for customer: {}", request.getCustomerName());
            return new OrderResponse(null, OrderStatus.PENDING); // Handle null case to prevent NPE
        }
        OrderResponse response = new OrderResponse();
        response.setCustomerOrderId(request.getCustomerOrderId());
        response.setVehicleModelId(request.getVehicleModelId());
        response.setVehicleVariantId(request.getVehicleVariantId());
        response.setCustomerName(request.getCustomerName());
        response.setPhoneNumber(request.getPhoneNumber());
        response.setEmail(request.getEmail());
        response.setPermanentAddress(request.getPermanentAddress());
        response.setCurrentAddress(request.getCurrentAddress());
        response.setAadharNo(request.getAadharNo());
        response.setPanNo(request.getPanNo());
        response.setModelName(request.getModelName());
        response.setFuelType(request.getFuelType());
        response.setColour(request.getColour());
        response.setTransmissionType(request.getTransmissionType());
        response.setVariant(request.getVariant());
        response.setQuantity(request.getQuantity());
        response.setTotalPrice(request.getTotalPrice());
        response.setBookingAmount(request.getBookingAmount());
        response.setPaymentMode(request.getPaymentMode());
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    @Override
    public void cancelOrder(Long customerOrderId) { // Changed from orderId to customerOrderId
        log.info("Received cancel signal for customerOrderId: {}", customerOrderId);
        isCanceled = true;
    }
}