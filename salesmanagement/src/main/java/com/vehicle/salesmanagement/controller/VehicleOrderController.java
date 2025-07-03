package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apirequest.MultiOrderRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.OrderStatsResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoGridResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.MultiOrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleOrderGridDTO;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.VehicleModelRepository;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.repository.VehicleVariantRepository;
import com.vehicle.salesmanagement.service.OrderIdGeneratorService;
import com.vehicle.salesmanagement.service.VehicleOrderService;
import com.vehicle.salesmanagement.workflow.VehicleSalesParentWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Vehicle Order Management")
public class VehicleOrderController {

    private final @Qualifier("workflowClient") WorkflowClient workflowClient;
    private final VehicleOrderDetailsRepository orderRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleVariantRepository vehicleVariantRepository;
    private final VehicleOrderService vehicleOrderService;
    private final ObjectMapper objectMapper;
    private final OrderIdGeneratorService orderIdGeneratorService;

    @PostMapping("/placeOrder")
    @Operation(
            summary = "Place vehicle order(s)",
            description = "Initiates one or multiple vehicle orders and starts workflows for each",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Vehicle order request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    oneOf = {OrderRequest.class, MultiOrderRequest.class}
                            ),
                            examples = {
                                    @ExampleObject(
                                            name = "SingleOrder",
                                            summary = "Single Order Example",
                                            description = "Data type example for a single vehicle order",
                                            value = "{\n" +
                                                    "  \"vehicleModelId\": \"integer\",\n" +
                                                    "  \"vehicleVariantId\": \"integer\",\n" +
                                                    "  \"customerName\": \"string\",\n" +
                                                    "  \"phoneNumber\": \"string\",\n" +
                                                    "  \"email\": \"string\",\n" +
                                                    "  \"permanentAddress\": \"string\",\n" +
                                                    "  \"currentAddress\": \"string\",\n" +
                                                    "  \"aadharNo\": \"string\",\n" +
                                                    "  \"panNo\": \"string\",\n" +
                                                    "  \"modelName\": \"string\",\n" +
                                                    "  \"fuelType\": \"string\",\n" +
                                                    "  \"colour\": \"string\",\n" +
                                                    "  \"transmissionType\": \"string\",\n" +
                                                    "  \"variant\": \"string\",\n" +
                                                    "  \"quantity\": \"integer\",\n" +
                                                    "  \"paymentMode\": \"string\",\n" +
                                                    "  \"expectedDeliveryDate\": \"string\"\n" +
                                                    "}"
                                    ),
                                    @ExampleObject(
                                            name = "MultiOrder",
                                            summary = "Multiple Orders Example",
                                            description = "Data type example for multiple vehicle orders",
                                            value = "{\n" +
                                                    "  \"vehicleOrders\": [\n" +
                                                    "    {\n" +
                                                    "      \"vehicleModelId\": \"integer\",\n" +
                                                    "      \"vehicleVariantId\": \"integer\",\n" +
                                                    "      \"customerName\": \"string\",\n" +
                                                    "      \"phoneNumber\": \"string\",\n" +
                                                    "      \"email\": \"string\",\n" +
                                                    "      \"permanentAddress\": \"string\",\n" +
                                                    "      \"currentAddress\": \"string\",\n" +
                                                    "      \"aadharNo\": \"string\",\n" +
                                                    "      \"panNo\": \"string\",\n" +
                                                    "      \"modelName\": \"string\",\n" +
                                                    "      \"fuelType\": \"string\",\n" +
                                                    "27;      \"colour\": \"string\",\n" +
                                                    "      \"transmissionType\": \"string\",\n" +
                                                    "      \"variant\": \"string\",\n" +
                                                    "      \"quantity\": \"integer\",\n" +
                                                    "      \"paymentMode\": \"string\",\n" +
                                                    "      \"expectedDeliveryDate\": \"string\"\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            }
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Order(s) successfully placed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)))
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> placeOrder(@RequestBody Object request) {
        try {
            String rawRequest = objectMapper.writeValueAsString(request);
            log.info("Received raw request: {}", rawRequest);

            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();

            if (request instanceof LinkedHashMap || request instanceof Map) {
                MultiOrderRequest multiOrderRequest = objectMapper.convertValue(request, MultiOrderRequest.class);
                if (multiOrderRequest.getVehicleOrders() != null && !multiOrderRequest.getVehicleOrders().isEmpty()) {
                    for (OrderRequest order : multiOrderRequest.getVehicleOrders()) {
                        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(order);
                        if (!violations.isEmpty()) {
                            throw new ConstraintViolationException(violations);
                        }
                    }
                    log.info("Deserialized as MultiOrderRequest with {} orders", multiOrderRequest.getVehicleOrders().size());
                    return handleMultiOrder(multiOrderRequest);
                }

                OrderRequest orderRequest = objectMapper.convertValue(request, OrderRequest.class);
                Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
                log.info("Deserialized as OrderRequest");
                return handleSingleOrder(orderRequest);
            }

            if (request instanceof OrderRequest orderRequest) {
                Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
                log.info("Received single OrderRequest: {}", orderRequest);
                return handleSingleOrder(orderRequest);
            } else if (request instanceof MultiOrderRequest multiOrderRequest) {
                for (OrderRequest order : multiOrderRequest.getVehicleOrders()) {
                    Set<ConstraintViolation<OrderRequest>> violations = validator.validate(order);
                    if (!violations.isEmpty()) {
                        throw new ConstraintViolationException(violations);
                    }
                }
                log.info("Received MultiOrderRequest with {} vehicle orders", multiOrderRequest.getVehicleOrders().size());
                return handleMultiOrder(multiOrderRequest);
            } else {
                log.error("Invalid request type: {}", request.getClass().getName());
                throw new IllegalArgumentException("Request must be either OrderRequest or MultiOrderRequest");
            }
        } catch (ConstraintViolationException e) {
            String errorMessage = e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            log.error("Validation failed: {}", errorMessage);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Validation error: " + errorMessage,
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (IllegalArgumentException e) {
            log.error("Deserialization or validation failed: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid request: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        } catch (Exception e) {
            log.error("Unexpected error processing request: {}", e.getMessage(), e);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    private ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> handleSingleOrder(@Valid OrderRequest orderRequest) {
        // Generate and set customerOrderId before mapping
        String customerOrderId = orderIdGeneratorService.generateCustomerOrderId();
        orderRequest.setCustomerOrderId(customerOrderId);
        log.debug("Generated and set customerOrderId: {}", customerOrderId);

        VehicleOrderDetails orderDetails = mapOrderRequestToEntity(orderRequest);
        orderDetails = orderRepository.saveAndFlush(orderDetails);

        if (customerOrderId == null) {
            log.error("CustomerOrderId is null after saving order for customer: {}", orderRequest.getCustomerName());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to generate customerOrderId for order",
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
        log.info("Order saved with customerOrderId: {}", customerOrderId);

        // Start the VehicleSalesParentWorkflow
        String workflowId = "parent-" + customerOrderId;
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("vehicle-order-task-queue")
                .setWorkflowId(workflowId)
                .setWorkflowExecutionTimeout(Duration.ofDays(7))
                .build();

        log.info("Attempting to start VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}", workflowId, customerOrderId);
        VehicleSalesParentWorkflow parentWorkflow;
        OrderResponse response;
        try {
            parentWorkflow = workflowClient.newWorkflowStub(VehicleSalesParentWorkflow.class, options);
            WorkflowExecution execution = WorkflowClient.start(parentWorkflow::processOrder, orderRequest);
            log.info("Successfully started VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}, runId: {}", workflowId, customerOrderId, execution.getRunId());

            Thread.sleep(3000);

            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId, Optional.of(execution.getRunId()), Optional.empty());
            int attempts = 0;
            int maxAttempts = 5;
            long delayBetweenAttempts = 1000;
            response = null;

            while (attempts < maxAttempts) {
                try {
                    response = workflowStub.query("getOrderStatus", OrderResponse.class);
                    if (response != null) {
                        log.info("Successfully retrieved status {} for customerOrderId: {}", response.getOrderStatus(), customerOrderId);
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Query attempt {}/{} failed for workflow ID: {}. Error: {}", attempts + 1, maxAttempts, workflowId, e.getMessage(), e);
                }
                attempts++;
                if (attempts < maxAttempts) {
                    Thread.sleep(delayBetweenAttempts);
                }
            }

            if (response == null) {
                log.warn("Could not retrieve status for customerOrderId: {} after {} attempts. Defaulting to workflow's initial status.", customerOrderId, maxAttempts);
                response = parentWorkflow.getOrderStatus();
            }
        } catch (Exception e) {
            log.error("Failed to start or query VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}. Error: {}", workflowId, customerOrderId, e.getMessage(), e);
            orderDetails.setOrderStatus(OrderStatus.FAILED);
            orderRepository.save(orderDetails);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to start or query parent workflow for customerOrderId: " + customerOrderId + ". Order marked as FAILED. Error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }

        orderDetails.setOrderStatus(response.getOrderStatus());
        orderRepository.save(orderDetails);

        response = mapOrderDetailsToResponse(orderDetails, response.getOrderStatus());

        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                HttpStatus.ACCEPTED.value(),
                "Order placed successfully with customerOrderId: " + customerOrderId + ". Parent workflow started.",
                response
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    private ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> handleMultiOrder(@Valid MultiOrderRequest multiOrderRequest) {
        // Validate vehicleModelId and vehicleVariantId for all orders
        for (int i = 0; i < multiOrderRequest.getVehicleOrders().size(); i++) {
            OrderRequest orderRequest = multiOrderRequest.getVehicleOrders().get(i);
            if (!vehicleModelRepository.existsById(orderRequest.getVehicleModelId())) {
                log.error("Vehicle Model with ID {} does not exist for customer: {} at order index: {}",
                        orderRequest.getVehicleModelId(), orderRequest.getCustomerName(), i + 1);
                com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Invalid vehicleModelId: " + orderRequest.getVehicleModelId() + " does not exist at order index: " + (i + 1),
                        null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }
            if (!vehicleVariantRepository.existsById(orderRequest.getVehicleVariantId())) {
                log.error("Vehicle Variant with ID {} does not exist for customer: {} at order index: {}",
                        orderRequest.getVehicleVariantId(), orderRequest.getCustomerName(), i + 1);
                com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Invalid vehicleVariantId: " + orderRequest.getVehicleVariantId() + " does not exist at order index: " + (i + 1),
                        null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
            }
        }

        // Generate unique customerOrderIds with retry mechanism
        List<String> customerOrderIds = new ArrayList<>();
        int maxRetries = 3;
        int attempt = 1;
        boolean success = false;

        while (attempt <= maxRetries && !success) {
            customerOrderIds.clear();
            log.info("Attempt {}/{} to generate unique customerOrderIds for {} orders", attempt, maxRetries, multiOrderRequest.getVehicleOrders().size());

            for (int i = 0; i < multiOrderRequest.getVehicleOrders().size(); i++) {
                String customerOrderId = orderIdGeneratorService.generateCustomerOrderId();
                customerOrderIds.add(customerOrderId);
                log.info("Pre-generated customerOrderId {} for order {}/{}", customerOrderId, i + 1, multiOrderRequest.getVehicleOrders().size());
            }

            // Check for duplicates
            Set<String> uniqueOrderIds = new HashSet<>(customerOrderIds);
            if (uniqueOrderIds.size() == customerOrderIds.size()) {
                success = true;
                log.info("Successfully generated unique customerOrderIds: {}", customerOrderIds);
            } else {
                log.warn("Duplicate customerOrderIds detected on attempt {}/{}: {}. Expected {} unique IDs, but found {}",
                        attempt, maxRetries, customerOrderIds, customerOrderIds.size(), uniqueOrderIds.size());
                attempt++;
                if (attempt <= maxRetries) {
                    try {
                        Thread.sleep(100); // Small delay before retrying
                    } catch (InterruptedException e) {
                        log.warn("Retry delay interrupted: {}", e.getMessage());
                    }
                }
            }
        }

        if (!success) {
            log.error("Failed to generate unique customerOrderIds after {} attempts: {}. Expected {} unique IDs, but found {}",
                    maxRetries, customerOrderIds, customerOrderIds.size(), new HashSet<>(customerOrderIds).size());
            throw new IllegalStateException("Failed to generate unique customerOrderIds for all orders");
        }

        List<OrderResponse> orderResponses = new ArrayList<>();
        List<String> failedCustomerOrderIds = new ArrayList<>();
        int orderIndex = 1;

        // Now process each order with the pre-generated customerOrderId
        for (int i = 0; i < multiOrderRequest.getVehicleOrders().size(); i++) {
            OrderRequest orderRequest = multiOrderRequest.getVehicleOrders().get(i);
            String customerOrderId = customerOrderIds.get(i);

            log.info("Processing order {}/{} for customer: {} and model: {}",
                    orderIndex, multiOrderRequest.getVehicleOrders().size(),
                    orderRequest.getCustomerName(), orderRequest.getModelName());

            // Set the customerOrderId in the OrderRequest before mapping
            orderRequest.setCustomerOrderId(customerOrderId);
            log.debug("Set customerOrderId {} on OrderRequest for customer: {}", customerOrderId, orderRequest.getCustomerName());

            VehicleOrderDetails orderDetails;
            try {
                orderDetails = mapOrderRequestToEntity(orderRequest);
            } catch (Exception e) {
                log.error("Failed to map order request at index {} for customer: {}. Error: {}",
                        orderIndex, orderRequest.getCustomerName(), e.getMessage(), e);
                failedCustomerOrderIds.add(customerOrderId);
                orderIndex++;
                continue;
            }

            // Ensure the customerOrderId is set correctly (redundant but safe)
            orderDetails.setCustomerOrderId(customerOrderId);
            orderDetails = orderRepository.saveAndFlush(orderDetails);

            if (orderDetails.getCustomerOrderId() == null || !orderDetails.getCustomerOrderId().equals(customerOrderId)) {
                log.error("CustomerOrderId mismatch after saving order for customer: {} at index: {}. Expected: {}, Found: {}",
                        orderRequest.getCustomerName(), orderIndex, customerOrderId, orderDetails.getCustomerOrderId());
                failedCustomerOrderIds.add(customerOrderId);
                orderIndex++;
                continue;
            }

            log.info("Order {}/{} saved with customerOrderId: {} for model: {}",
                    orderIndex, multiOrderRequest.getVehicleOrders().size(),
                    customerOrderId, orderRequest.getModelName());

            String workflowId = "parent-" + customerOrderId;
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("vehicle-order-task-queue")
                    .setWorkflowId(workflowId)
                    .setWorkflowExecutionTimeout(Duration.ofDays(7))
                    .build();

            log.info("Attempting to start VehicleSalesParentWorkflow with ID: {} for customerOrderId: {} at index: {}",
                    workflowId, customerOrderId, orderIndex);
            VehicleSalesParentWorkflow parentWorkflow;
            OrderResponse response;
            try {
                parentWorkflow = workflowClient.newWorkflowStub(VehicleSalesParentWorkflow.class, options);
                WorkflowExecution execution = WorkflowClient.start(parentWorkflow::processOrder, orderRequest);
                log.info("Successfully started VehicleSalesParentWorkflow with ID: {} for customerOrderId: {} at index: {}, runId: {}",
                        workflowId, customerOrderId, orderIndex, execution.getRunId());

                Thread.sleep(3000);

                WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId, Optional.of(execution.getRunId()), Optional.empty());
                int attempts = 0;
                int maxAttempts = 5;
                long delayBetweenAttempts = 1000;
                response = null;

                while (attempts < maxAttempts) {
                    try {
                        response = workflowStub.query("getOrderStatus", OrderResponse.class);
                        if (response != null) {
                            log.info("Successfully retrieved status {} for customerOrderId: {} at index: {}",
                                    response.getOrderStatus(), customerOrderId, orderIndex);
                            break;
                        }
                    } catch (Exception e) {
                        log.warn("Query attempt {}/{} failed for workflow ID: {} at index: {}. Error: {}",
                                attempts + 1, maxAttempts, workflowId, orderIndex, e.getMessage(), e);
                    }
                    attempts++;
                    if (attempts < maxAttempts) {
                        Thread.sleep(delayBetweenAttempts);
                    }
                }

                if (response == null) {
                    log.warn("Could not retrieve status for customerOrderId: {} at index: {} after {} attempts. Defaulting to workflow's initial status.",
                            customerOrderId, orderIndex, maxAttempts);
                    response = parentWorkflow.getOrderStatus();
                }
            } catch (Exception e) {
                log.error("Failed to start or query VehicleSalesParentWorkflow with ID: {} for customerOrderId: {} at index: {}. Model: {}. Error: {}",
                        workflowId, customerOrderId, orderIndex, orderRequest.getModelName(), e.getMessage(), e);
                orderDetails.setOrderStatus(OrderStatus.FAILED);
                orderRepository.save(orderDetails);
                failedCustomerOrderIds.add(customerOrderId);
                orderIndex++;
                continue;
            }

            orderDetails.setOrderStatus(response.getOrderStatus());
            orderRepository.save(orderDetails);

            response = mapOrderDetailsToResponse(orderDetails, response.getOrderStatus());
            orderResponses.add(response);
            orderIndex++;
        }

        if (!failedCustomerOrderIds.isEmpty()) {
            log.warn("Some orders failed to start: {}", failedCustomerOrderIds);
            if (orderResponses.isEmpty()) {
                com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Failed to place orders: " + failedCustomerOrderIds,
                        null
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
            }
        }

        MultiOrderResponse multiOrderResponse = new MultiOrderResponse(
                HttpStatus.ACCEPTED.value(),
                "Orders placed successfully with customerOrderIds: " + customerOrderIds.stream()
                        .filter(id -> !failedCustomerOrderIds.contains(id))
                        .collect(Collectors.toList()),
                orderResponses
        );

        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                HttpStatus.ACCEPTED.value(),
                "Multiple orders placed successfully. Parent workflows started. Failed orders: " + (failedCustomerOrderIds.isEmpty() ? "None" : failedCustomerOrderIds),
                multiOrderResponse
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    @PostMapping("/cancelOrder")
    @Operation(summary = "Cancel a vehicle order", description = "Cancels an existing vehicle order by customerOrderId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order canceled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid customerOrderId"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> cancelOrder(@Valid @RequestParam String customerOrderId) {
        log.info("Canceling order with customerOrderId: {}", customerOrderId);
        try {
            VehicleSalesParentWorkflow parentWorkflow = workflowClient.newWorkflowStub(
                    VehicleSalesParentWorkflow.class,
                    "parent-" + customerOrderId
            );
            parentWorkflow.cancelOrder(customerOrderId);

            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("vehicle-order-task-queue")
                    .setWorkflowId("cancel-order-" + customerOrderId)
                    .build();

            OrderResponse response = workflowClient.newUntypedWorkflowStub("cancel-order-" + customerOrderId)
                    .getResult(10, TimeUnit.SECONDS, OrderResponse.class);

            log.info("Cancellation workflow started and completed successfully for customerOrderId: {}", customerOrderId);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Order canceled successfully with customerOrderId: " + customerOrderId,
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to start or complete cancellation workflow for customerOrderId: {} - {}", customerOrderId, e.getMessage());
            OrderResponse response = vehicleOrderService.cancelOrder(customerOrderId);
            log.info("Fallback: Order canceled directly with customerOrderId: {}", customerOrderId);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Order canceled successfully with customerOrderId: " + customerOrderId + " (via fallback)",
                    response
            );
            return ResponseEntity.ok(apiResponse);
        }
    }

    @GetMapping("/orderStats")
    @Operation(summary = "Get order statistics", description = "Returns total, pending, finance pending, and closed order counts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order stats retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getOrderStats() {
        try {
            long total = vehicleOrderService.getTotalOrders();
            long pending = vehicleOrderService.getPendingOrders();
            long financePending = vehicleOrderService.getFinancePendingOrders();
            long closed = vehicleOrderService.getClosedOrders();

            OrderStatsResponse statsResponse = new OrderStatsResponse(total, pending, financePending, closed);

            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse =
                    new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                            HttpStatus.OK.value(),
                            "Order stats retrieved successfully",
                            statsResponse
                    );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to retrieve order stats: {}", e.getMessage(), e);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse =
                    new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Internal server error: " + e.getMessage(),
                            null
                    );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/vehicleorders")
    @Operation(
            summary = "Get all vehicle orders for Kendo Grid",
            description = "Fetches all customer vehicle orders to be displayed in a Kendo UI Grid.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful retrieval of vehicle orders",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = KendoGridResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<KendoGridResponse<VehicleOrderGridDTO>> getVehicleOrdersForGrid() {
        try {
            List<VehicleOrderGridDTO> gridData = vehicleOrderService.getAllOrders();
            log.info("Retrieved {} vehicle orders for Kendo Grid", gridData.size());
            return ResponseEntity.ok(new KendoGridResponse<>(gridData, (long) gridData.size(), null, null));
        } catch (Exception e) {
            log.error("Failed to retrieve vehicle orders for Kendo Grid: {}", e.getMessage(), e);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new KendoGridResponse<>(Collections.emptyList(), 0L, "Error retrieving vehicle orders: " + e.getMessage(), null));
        }
    }

    @Transactional
    private VehicleOrderDetails mapOrderRequestToEntity(OrderRequest request) {
        try {
            VehicleOrderDetails order = new VehicleOrderDetails();
            log.debug("Mapping OrderRequest for customer: {} and model: {}", request.getCustomerName(), request.getModelName());

            // These should already be validated, but keeping the checks for safety
            order.setVehicleModelId(vehicleModelRepository.findById(request.getVehicleModelId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle Model with ID " + request.getVehicleModelId() + " not found")));
            order.setVehicleVariantId(vehicleVariantRepository.findById(request.getVehicleVariantId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle Variant with ID " + request.getVehicleVariantId() + " not found")));

            // Set customerOrderId from the request
            order.setCustomerOrderId(request.getCustomerOrderId());
            log.debug("Set customerOrderId {} on VehicleOrderDetails for customer: {}", request.getCustomerOrderId(), request.getCustomerName());

            order.setCustomerName(request.getCustomerName());
            order.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "");
            order.setEmail(request.getEmail());
            order.setPermanentAddress(request.getPermanentAddress());
            order.setCurrentAddress(request.getCurrentAddress());
            order.setAadharNo(request.getAadharNo());
            order.setPanNo(request.getPanNo());
            order.setModelName(request.getModelName());
            order.setFuelType(request.getFuelType());
            order.setColour(request.getColour());
            order.setTransmissionType(request.getTransmissionType());
            order.setVariant(request.getVariant());
            order.setQuantity(request.getQuantity());
            order.setPaymentMode(request.getPaymentMode());
            order.setOrderStatus(OrderStatus.PENDING);
            order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
            log.debug("Mapped OrderRequest to VehicleOrderDetails with customerOrderId: {}", order.getCustomerOrderId());

            // Validate customerOrderId
            if (order.getCustomerOrderId() == null || order.getCustomerOrderId().isEmpty()) {
                log.error("CustomerOrderId is null or empty for customer: {}. Request customerOrderId was: {}",
                        request.getCustomerName(), request.getCustomerOrderId());
                throw new IllegalStateException("CustomerOrderId must be set before saving order");
            }

            return order;
        } catch (IllegalArgumentException e) {
            log.error("Validation error during mapping for customer: {}. Error: {}", request.getCustomerName(), e.getMessage(), e);
            throw new RuntimeException("Failed to map order request: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during mapping for customer: {}. Request: {}. Error: {}",
                    request.getCustomerName(), request, e.getMessage(), e);
            throw new RuntimeException("Failed to map order request: " + e.getMessage(), e);
        }
    }

    @GetMapping("/orderstatus/{orderId}")
    public ResponseEntity<KendoGridResponse<OrderResponse>> getOrderStatusProgress(@PathVariable String orderId) {
        return orderRepository.findByCustomerOrderId(orderId)
                .map(order -> {
                    OrderResponse response = mapOrderDetailsToResponse(order, order.getOrderStatus());
                    List<OrderResponse> result = List.of(response);
                    return ResponseEntity.ok(new KendoGridResponse<>(result, result.size(), null, null));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new KendoGridResponse<>(Collections.emptyList(), 0, null, null)));
    }

    private OrderResponse mapOrderDetailsToResponse(VehicleOrderDetails orderDetails, OrderStatus status) {
        OrderResponse order = new OrderResponse();
        order.setCustomerOrderId(orderDetails.getCustomerOrderId());
        order.setOrderStatus(status);
        order.setVehicleModelId(orderDetails.getVehicleModelId() != null ? orderDetails.getVehicleModelId().getVehicleModelId() : null);
        order.setVehicleVariantId(orderDetails.getVehicleVariantId() != null ? orderDetails.getVehicleVariantId().getVehicleVariantId() : null);
        order.setCustomerName(orderDetails.getCustomerName());
        order.setPhoneNumber(orderDetails.getPhoneNumber());
        order.setEmail(orderDetails.getEmail());
        order.setPermanentAddress(orderDetails.getPermanentAddress());
        order.setCurrentAddress(orderDetails.getCurrentAddress());
        order.setAadharNo(orderDetails.getAadharNo());
        order.setPanNo(orderDetails.getPanNo());
        order.setModelName(orderDetails.getModelName());
        order.setFuelType(orderDetails.getFuelType());
        order.setColour(orderDetails.getColour());
        order.setTransmissionType(orderDetails.getTransmissionType());
        order.setVariant(orderDetails.getVariant());
        order.setQuantity(orderDetails.getQuantity());
        order.setPaymentMode(orderDetails.getPaymentMode());
        order.setExpectedDeliveryDate(orderDetails.getExpectedDeliveryDate());
        return order;
    }
}