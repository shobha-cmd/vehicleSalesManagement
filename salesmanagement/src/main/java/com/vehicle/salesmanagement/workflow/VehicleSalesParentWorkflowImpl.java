package com.vehicle.salesmanagement.workflow;

import com.vehicle.salesmanagement.domain.dto.apirequest.DeliveryRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.DispatchRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DeliveryResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.DispatchResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.activity.DispatchDeliveryActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class VehicleSalesParentWorkflowImpl implements VehicleSalesParentWorkflow {

    private final Map<String, String> workflowStatuses = new HashMap<>();
    private boolean isFinanceInitiated = false;
    private boolean isDispatchInitiated = false;
    private boolean isDeliveryConfirmed = false;
    private boolean isCanceled = false;
    private Long customerOrderId;
    private FinanceRequest financeRequest;
    private DispatchRequest dispatchRequest;
    private DeliveryRequest deliveryRequest;

    private OrderResponse latestOrderResponse;

    private final DispatchDeliveryActivities dispatchDeliveryActivities;

    public VehicleSalesParentWorkflowImpl() {
        ActivityOptions activityOptions = ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setInitialInterval(Duration.ofSeconds(1))
                        .setMaximumAttempts(3)
                        .build())
                .build();
        this.dispatchDeliveryActivities = Workflow.newActivityStub(DispatchDeliveryActivities.class, activityOptions);
    }

    @Override
    public OrderResponse processOrder(OrderRequest orderRequest) {
        log.info("Parent Workflow started for customer: {} with customerOrderId: {}", orderRequest.getCustomerName(), orderRequest.getCustomerOrderId());

        workflowStatuses.put("Order", "BLOCKED");
        workflowStatuses.put("Finance", "PENDING");
        workflowStatuses.put("Dispatch-Delivery", "PENDING");

        ChildWorkflowOptions orderOptions = ChildWorkflowOptions.newBuilder()
                .setWorkflowId("order-" + Workflow.getInfo().getWorkflowId())
                .setTaskQueue("vehicle-order-task-queue")
                .build();
        VehicleOrderWorkflow orderWorkflow = Workflow.newChildWorkflowStub(VehicleOrderWorkflow.class, orderOptions);
        OrderResponse orderResponse;
        try {
            orderResponse = orderWorkflow.placeOrder(orderRequest);
        } catch (Exception e) {
            log.error("VehicleOrderWorkflow failed for customer: {}. Error: {}", orderRequest.getCustomerName(), e.getMessage(), e);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            return new OrderResponse(orderRequest.getCustomerOrderId(), OrderStatus.FAILED);
        }

        customerOrderId = orderResponse.getCustomerOrderId();
        if (customerOrderId == null) {
            log.error("CustomerOrderId is null after VehicleOrderWorkflow for customer: {}", orderRequest.getCustomerName());
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            return orderResponse;
        }
        workflowStatuses.put("Order", orderResponse.getOrderStatus().name());
        log.info("Order Workflow completed with status: {} for customerOrderId: {}", orderResponse.getOrderStatus(), customerOrderId);

        if (isCanceled) {
            log.info("Order canceled during parent workflow for customerOrderId: {}", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.CANCELED.name());
            orderResponse.setOrderStatus(OrderStatus.CANCELED);
            return orderResponse;
        }

        log.info("Waiting for finance initiation signal for customerOrderId: {}", customerOrderId);
        boolean financeAwaitResult = Workflow.await(Duration.ofDays(7), () -> {
            log.debug("Checking finance initiation condition: isCanceled={}, isFinanceInitiated={}", isCanceled, isFinanceInitiated);
            return isCanceled || isFinanceInitiated;
        });
        if (!financeAwaitResult) {
            log.warn("Finance initiation timed out after 7 days for customerOrderId: {}. Marking as FAILED.", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }
        if (isCanceled) {
            log.info("Parent Workflow canceled before finance initiation for customerOrderId: {}", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.CANCELED.name());
            orderResponse.setOrderStatus(OrderStatus.CANCELED);
            return orderResponse;
        }

        if (!isFinanceInitiated) {
            log.warn("Finance initiation not received for customerOrderId: {}. Marking as FAILED.", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }

        ChildWorkflowOptions financeOptions = ChildWorkflowOptions.newBuilder()
                .setWorkflowId("finance-" + customerOrderId)
                .setTaskQueue("finance-task-queue")
                .build();
        FinanceWorkflow financeWorkflow = Workflow.newChildWorkflowStub(FinanceWorkflow.class, financeOptions);
        try {
            Workflow.newDetachedCancellationScope(() -> financeWorkflow.processFinance(financeRequest)).run();
            log.info("Finance Workflow started with PENDING status for customerOrderId: {}. WorkflowId: finance-{}", customerOrderId, customerOrderId);
        } catch (Exception e) {
            log.error("Finance Workflow failed to start for customerOrderId: {}. Error: {}", customerOrderId, e.getMessage(), e);
            workflowStatuses.put("Finance", "FAILED");
            orderResponse.setOrderStatus(OrderStatus.PENDING);
            return orderResponse;
        }

        // Modified: Only update orderStatus based on finance outcome, not default to PENDING
        String financeStatus = workflowStatuses.get("Finance");
        if ("APPROVED".equals(financeStatus)) {
            orderResponse.setOrderStatus(OrderStatus.ALLOTTED);
        } else if ("REJECTED".equals(financeStatus)) {
            orderResponse.setOrderStatus(OrderStatus.PENDING);
        } else {
            log.info("Finance Workflow still in progress for customerOrderId: {}. Retaining order status: {}", customerOrderId, orderResponse.getOrderStatus());
            // Do not overwrite orderStatus to PENDING; retain existing status (e.g., BLOCKED)
        }

        log.info("Waiting for dispatch initiation for customerOrderId: {}", customerOrderId);
        boolean dispatchAwaitResult = Workflow.await(Duration.ofDays(7), () -> {
            log.debug("Checking dispatch initiation condition: isCanceled={}, isDispatchInitiated={}", isCanceled, isDispatchInitiated);
            return isCanceled || isDispatchInitiated;
        });
        if (!dispatchAwaitResult) {
            log.warn("Dispatch initiation timed out after 7 days for customerOrderId: {}. Marking as FAILED.", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            workflowStatuses.put("Finance", workflowStatuses.get("Finance"));
            workflowStatuses.put("Dispatch-Delivery", "FAILED");
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }
        if (isCanceled) {
            log.info("Parent Workflow canceled before dispatch initiation for customerOrderId: {}", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.CANCELED.name());
            workflowStatuses.put("Finance", "CANCELED");
            workflowStatuses.put("Dispatch-Delivery", "CANCELED");
            orderResponse.setOrderStatus(OrderStatus.CANCELED);
            return orderResponse;
        }

        if (!isDispatchInitiated) {
            log.warn("Dispatch initiation not received for customerOrderId: {}. Marking as FAILED.", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            workflowStatuses.put("Finance", workflowStatuses.get("Finance"));
            workflowStatuses.put("Dispatch-Delivery", "FAILED");
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }

        try {
            Optional<VehicleOrderDetails> orderDetailsOpt = dispatchDeliveryActivities.getVehicleOrderDetails(customerOrderId);
            if (orderDetailsOpt.isPresent()) {
                VehicleOrderDetails orderDetails = orderDetailsOpt.get();
                OrderStatus currentStatus = orderDetails.getOrderStatus();
                if (currentStatus.equals(OrderStatus.ALLOTTED)) {
                    DispatchResponse dispatchResponse = dispatchDeliveryActivities.initiateDispatch(dispatchRequest);
                    if (!dispatchResponse.getOrderStatus().equals(OrderStatus.DISPATCHED)) {
                        log.error("Dispatch failed to set order status to DISPATCHED for customerOrderId: {}", customerOrderId);
                        throw new RuntimeException("Dispatch failed for customerOrderId: " + customerOrderId);
                    }
                    log.info("Dispatch completed, order status updated to DISPATCHED for customerOrderId: {}", customerOrderId);
                } else if (currentStatus.equals(OrderStatus.DISPATCHED) ||
                        currentStatus.equals(OrderStatus.DELIVERED) ||
                        currentStatus.equals(OrderStatus.COMPLETED)) {
                    log.warn("CustomerOrderId {} is already in status {}. Skipping dispatch.", customerOrderId, currentStatus);
                } else {
                    log.error("CustomerOrderId {} is in invalid status {} for dispatch.", customerOrderId, currentStatus);
                    throw new RuntimeException("Invalid order status for dispatch: " + currentStatus);
                }
            } else {
                log.error("CustomerOrderId {} not found for dispatch.", customerOrderId);
                throw new RuntimeException("Order not found: " + customerOrderId);
            }
        } catch (Exception e) {
            log.error("Dispatch failed for customerOrderId: {}. Error: {}", customerOrderId, e.getMessage(), e);
            workflowStatuses.put("Dispatch-Delivery", "FAILED");
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }

        log.info("Waiting for delivery confirmation for customerOrderId: {}", customerOrderId);
        boolean deliveryAwaitResult = Workflow.await(Duration.ofHours(1), () -> {
            log.debug("Checking delivery confirmation condition: isCanceled={}, isDeliveryConfirmed={}", isCanceled, isDeliveryConfirmed);
            return isCanceled || isDeliveryConfirmed;
        });
        if (!deliveryAwaitResult) {
            log.warn("Delivery confirmation timed out after 1 hour for customerOrderId: {}. Marking as FAILED.", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            workflowStatuses.put("Finance", workflowStatuses.get("Finance"));
            workflowStatuses.put("Dispatch-Delivery", "FAILED");
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }
        if (isCanceled) {
            log.info("Parent Workflow canceled before delivery confirmation for customerOrderId: {}", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.CANCELED.name());
            workflowStatuses.put("Finance", "CANCELED");
            workflowStatuses.put("Dispatch-Delivery", "CANCELED");
            orderResponse.setOrderStatus(OrderStatus.CANCELED);
            return orderResponse;
        }

        if (!isDeliveryConfirmed) {
            log.warn("Delivery confirmation not received for customerOrderId: {}. Marking as FAILED.", customerOrderId);
            orderWorkflow.cancelOrder(customerOrderId);
            workflowStatuses.put("Order", OrderStatus.FAILED.name());
            workflowStatuses.put("Finance", workflowStatuses.get("Finance"));
            workflowStatuses.put("Dispatch-Delivery", "FAILED");
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }

        try {
            Optional<VehicleOrderDetails> orderDetailsOpt = dispatchDeliveryActivities.getVehicleOrderDetails(customerOrderId);
            if (orderDetailsOpt.isPresent()) {
                OrderStatus currentStatus = orderDetailsOpt.get().getOrderStatus();
                if (currentStatus.equals(OrderStatus.DISPATCHED)) {
                    DeliveryResponse deliveryResponse = dispatchDeliveryActivities.confirmDelivery(deliveryRequest);
                    if (!deliveryResponse.getOrderStatus().equals(OrderStatus.DELIVERED)) {
                        log.error("Delivery confirmation failed to set order status to DELIVERED for customerOrderId: {}", customerOrderId);
                        throw new RuntimeException("Delivery confirmation failed for customerOrderId: " + customerOrderId);
                    }
                    log.info("Delivery completed, order status updated to DELIVERED for customerOrderId: {}", customerOrderId);
                    workflowStatuses.put("Dispatch-Delivery", "DELIVERED");
                } else if (currentStatus.equals(OrderStatus.DELIVERED) ||
                        currentStatus.equals(OrderStatus.COMPLETED)) {
                    log.warn("CustomerOrderId {} is already in status {}. Skipping delivery confirmation.", customerOrderId, currentStatus);
                    workflowStatuses.put("Dispatch-Delivery", "DELIVERED");
                } else {
                    log.error("CustomerOrderId {} is in invalid status {} for delivery.", customerOrderId, currentStatus);
                    throw new RuntimeException("Invalid order status for delivery: " + currentStatus);
                }
            } else {
                log.error("CustomerOrderId {} not found for delivery confirmation.", customerOrderId);
                throw new RuntimeException("Order not found: " + customerOrderId);
            }
        } catch (Exception e) {
            log.error("Delivery confirmation failed for customerOrderId: {}. Error: {}", customerOrderId, e.getMessage(), e);
            workflowStatuses.put("Dispatch-Delivery", "FAILED");
            orderResponse.setOrderStatus(OrderStatus.FAILED);
            return orderResponse;
        }

        if (workflowStatuses.get("Dispatch-Delivery").equals("DELIVERED")) {
            orderResponse.setOrderStatus(OrderStatus.DELIVERED);
        }

        log.info("Parent Workflow completed with statuses: {} for customerOrderId: {}", workflowStatuses, customerOrderId);
        return orderResponse;
    }

    @Override
    public void initiateFinance(FinanceRequest financeRequest) {
        log.info("Received signal to initiate Finance Workflow for customerOrderId: {}", financeRequest.getCustomerOrderId());
        this.customerOrderId = financeRequest.getCustomerOrderId();
        this.financeRequest = financeRequest;
        isFinanceInitiated = true;
        workflowStatuses.put("Finance", "INITIATED");
    }

    @Override
    public void approveFinance(String approvedBy) {
        log.info("Received signal to approve Finance Workflow for customerOrderId: {}", customerOrderId);
        workflowStatuses.put("Finance", "APPROVED");
        log.info("Finance Workflow approved for customerOrderId: {}", customerOrderId);
    }

    @Override
    public void rejectFinance(String rejectedBy) {
        log.info("Received signal to reject Finance Workflow for customerOrderId: {}", customerOrderId);
        workflowStatuses.put("Finance", "REJECTED");
        log.info("Finance Workflow rejected for customerOrderId: {}", customerOrderId);
    }

    @Override
    public void initiateDispatch(DispatchRequest dispatchRequest) {
        log.info("Received signal to initiate Dispatch for customerOrderId: {}", dispatchRequest.getCustomerOrderId());
        this.customerOrderId = dispatchRequest.getCustomerOrderId();
        this.dispatchRequest = dispatchRequest;
        isDispatchInitiated = true;
        workflowStatuses.put("Dispatch-Delivery", "DISPATCH_INITIATED");
        log.info("Dispatch initiated for customerOrderId: {}", customerOrderId);
    }

    @Override
    public void confirmDelivery(DeliveryRequest deliveryRequest) {
        log.info("Received signal to confirm Delivery for customerOrderId: {}", deliveryRequest.getCustomerOrderId());
        this.customerOrderId = deliveryRequest.getCustomerOrderId();
        this.deliveryRequest = deliveryRequest;
        isDeliveryConfirmed = true;
        log.info("Delivery confirmation signal received for customerOrderId: {}", customerOrderId);
    }

    @Override
    public void cancelOrder(Long customerOrderId) {
        log.info("Received cancel signal for parent workflow, customerOrderId: {}", customerOrderId);
        this.customerOrderId = customerOrderId;
        isCanceled = true;
        workflowStatuses.put("Order", "CANCELED");
        workflowStatuses.put("Finance", "CANCELED");
        workflowStatuses.put("Dispatch-Delivery", "CANCELED");
    }

    @Override
    public String getWorkflowStatus() {
        log.info("Querying workflow status for customerOrderId: {}. Current status: {}", customerOrderId, workflowStatuses.get("Order"));
        return workflowStatuses.getOrDefault("Order", "UNKNOWN");
    }

    @Override
    public OrderResponse getOrderStatus() {
        log.info("Fetching order status for customerOrderId: {}", customerOrderId);
        String currentStatus = workflowStatuses.getOrDefault("Order", OrderStatus.PENDING.name());

        if (customerOrderId == null) {
            log.error("customerOrderId is null in getOrderStatus, returning PENDING status");
            return new OrderResponse(null, OrderStatus.PENDING);
        }

        OrderStatus orderStatus;
        try {
            orderStatus = OrderStatus.valueOf(currentStatus);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {} for customerOrderId: {}, defaulting to PENDING", currentStatus, customerOrderId);
            orderStatus = OrderStatus.PENDING;
        }

        return new OrderResponse(customerOrderId, orderStatus);
    }
}