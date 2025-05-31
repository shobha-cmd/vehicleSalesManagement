package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.KendoGridResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.MultiOrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.OrderResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.VehicleOrderGridDTO;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.VehicleModelRepository;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.repository.VehicleVariantRepository;
import com.vehicle.salesmanagement.service.VehicleOrderService;
import com.vehicle.salesmanagement.workflow.VehicleCancelWorkflow;
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
import jakarta.transaction.Transactional;
import jakarta.validation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
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

    @Transactional
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
                                                    "  \"totalPrice\": \"number\",\n" +
                                                    "  \"bookingAmount\": \"number\",\n" +
                                                    "  \"paymentMode\": \"string\",\n" +
                                                    "  \"createdBy\": \"string\",\n" +
                                                    "  \"updatedBy\": \"string\"\n" +
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
                                                    "      \"colour\": \"string\",\n" +
                                                    "      \"transmissionType\": \"string\",\n" +
                                                    "      \"variant\": \"string\",\n" +
                                                    "      \"quantity\": \"integer\",\n" +
                                                    "      \"totalPrice\": \"number\",\n" +
                                                    "      \"bookingAmount\": \"number\",\n" +
                                                    "      \"paymentMode\": \"string\",\n" +
                                                    "      \"createdBy\": \"string\",\n" +
                                                    "      \"updatedBy\": \"string\"\n" +
                                                    "    },\n" +
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
                                                    "      \"colour\": \"string\",\n" +
                                                    "      \"transmissionType\": \"string\",\n" +
                                                    "      \"variant\": \"string\",\n" +
                                                    "      \"quantity\": \"integer\",\n" +
                                                    "      \"totalPrice\": \"number\",\n" +
                                                    "      \"bookingAmount\": \"number\",\n" +
                                                    "      \"paymentMode\": \"string\",\n" +
                                                    "      \"createdBy\": \"string\",\n" +
                                                    "      \"updatedBy\": \"string\"\n" +
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
        VehicleOrderDetails orderDetails = mapOrderRequestToEntity(orderRequest);
        orderDetails.setCreatedAt(LocalDateTime.now());
        orderDetails.setUpdatedAt(LocalDateTime.now());
        orderDetails = orderRepository.saveAndFlush(orderDetails);
        Long customerOrderId = orderDetails.getCustomerOrderId(); // Changed from orderId
        if (customerOrderId == null) {
            log.error("CustomerOrderId is null after saving order for customer: {}", orderRequest.getCustomerName());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to generate customerOrderId for order",
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
        log.info("Order saved with customerOrderId: {}", customerOrderId); // Updated log

        // Set the customerOrderId in the OrderRequest
        orderRequest.setCustomerOrderId(customerOrderId);

        // Start the VehicleSalesParentWorkflow
        String workflowId = "parent-" + customerOrderId;
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue("vehicle-order-task-queue")
                .setWorkflowId(workflowId)
                .setWorkflowExecutionTimeout(Duration.ofDays(7))
                .build();

        log.info("Attempting to start VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}", workflowId, customerOrderId); // Updated log
        VehicleSalesParentWorkflow parentWorkflow;
        OrderResponse response;
        try {
            parentWorkflow = workflowClient.newWorkflowStub(VehicleSalesParentWorkflow.class, options);
            WorkflowExecution execution = WorkflowClient.start(parentWorkflow::processOrder, orderRequest);
            log.info("Successfully started VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}, runId: {}", workflowId, customerOrderId, execution.getRunId()); // Updated log

            // Wait briefly to ensure the workflow has started and registered its query handlers
            Thread.sleep(3000); // Increased initial delay to 3 seconds to ensure workflow is queryable

            // Create a new workflow stub with the specific runId to ensure accurate querying
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId, Optional.of(execution.getRunId()), Optional.empty());
            int attempts = 0;
            int maxAttempts = 5;
            long delayBetweenAttempts = 1000; // 1 second delay between attempts
            response = null;

            while (attempts < maxAttempts) {
                try {
                    response = workflowStub.query("getOrderStatus", OrderResponse.class);
                    if (response != null) {
                        log.info("Successfully retrieved status {} for customerOrderId: {}", response.getOrderStatus(), customerOrderId); // Updated log
                        break; // Exit the loop as soon as we get a valid response
                    }
                } catch (Exception e) {
                    log.warn("Query attempt {}/{} failed for workflow ID: {}. Error: {}", attempts + 1, maxAttempts, workflowId, e.getMessage(), e);
                }
                attempts++;
                if (attempts < maxAttempts) {
                    Thread.sleep(delayBetweenAttempts); // Wait before retrying
                }
            }

            // If we couldn't get a response after retries, use the default status from the workflow
            if (response == null) {
                log.warn("Could not retrieve status for customerOrderId: {} after {} attempts. Defaulting to workflow's initial status.", customerOrderId, maxAttempts); // Updated log
                response = parentWorkflow.getOrderStatus(); // Fallback to the workflow's default status (BLOCKED or PENDING)
            }
        } catch (Exception e) {
            log.error("Failed to start or query VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}. Error: {}", workflowId, customerOrderId, e.getMessage(), e); // Updated log
            orderDetails.setOrderStatus(OrderStatus.FAILED); // Set status to FAILED instead of deleting
            orderRepository.save(orderDetails);
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to start or query parent workflow for customerOrderId: " + customerOrderId + ". Order marked as FAILED. Error: " + e.getMessage(), // Updated message
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }

        // Update the order status in the database
        orderDetails.setOrderStatus(response.getOrderStatus());
        orderRepository.save(orderDetails);

        // Populate the OrderResponse with details from VehicleOrderDetails
        response = mapOrderDetailsToResponse(orderDetails, response.getOrderStatus());

        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                HttpStatus.ACCEPTED.value(),
                "Order placed successfully with customerOrderId: " + customerOrderId + ". Parent workflow started.", // Updated message
                response
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    private ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> handleMultiOrder(@Valid MultiOrderRequest multiOrderRequest) {
        List<OrderResponse> orderResponses = new ArrayList<>();
        List<Long> customerOrderIds = new ArrayList<>(); // Changed from orderIds
        List<Long> failedCustomerOrderIds = new ArrayList<>(); // Changed from failedOrderIds

        for (OrderRequest orderRequest : multiOrderRequest.getVehicleOrders()) {
            VehicleOrderDetails orderDetails = mapOrderRequestToEntity(orderRequest);
            orderDetails.setCreatedAt(LocalDateTime.now());
            orderDetails.setUpdatedAt(LocalDateTime.now());
            orderDetails = orderRepository.saveAndFlush(orderDetails);
            final Long customerOrderId = orderDetails.getCustomerOrderId(); // Changed from orderId
            if (customerOrderId == null) {
                log.error("CustomerOrderId is null after saving order for customer: {}", orderRequest.getCustomerName());
                failedCustomerOrderIds.add(0L); // Add placeholder ID for tracking
                continue;
            }
            customerOrderIds.add(customerOrderId);
            log.info("Order saved with customerOrderId: {}", customerOrderId); // Updated log

            // Set the customerOrderId in the OrderRequest
            orderRequest.setCustomerOrderId(customerOrderId);

            // Start the VehicleSalesParentWorkflow for each order
            String workflowId = "parent-" + customerOrderId;
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("vehicle-order-task-queue")
                    .setWorkflowId(workflowId)
                    .setWorkflowExecutionTimeout(Duration.ofDays(7))
                    .build();

            log.info("Attempting to start VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}", workflowId, customerOrderId); // Updated log
            VehicleSalesParentWorkflow parentWorkflow;
            OrderResponse response;
            try {
                parentWorkflow = workflowClient.newWorkflowStub(VehicleSalesParentWorkflow.class, options);
                WorkflowExecution execution = WorkflowClient.start(parentWorkflow::processOrder, orderRequest);
                log.info("Successfully started VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}, runId: {}", workflowId, customerOrderId, execution.getRunId()); // Updated log

                // Wait briefly to ensure the workflow has started and registered its query handlers
                Thread.sleep(3000); // Increased initial delay to 3 seconds to ensure workflow is queryable

                // Create a new workflow stub with the specific runId to ensure accurate querying
                WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId, Optional.of(execution.getRunId()), Optional.empty());
                int attempts = 0;
                int maxAttempts = 5;
                long delayBetweenAttempts = 1000; // 1 second delay between attempts
                response = null;

                while (attempts < maxAttempts) {
                    try {
                        response = workflowStub.query("getOrderStatus", OrderResponse.class);
                        if (response != null) {
                            log.info("Successfully retrieved status {} for customerOrderId: {}", response.getOrderStatus(), customerOrderId); // Updated log
                            break; // Exit the loop as soon as we get a valid response
                        }
                    } catch (Exception e) {
                        log.warn("Query attempt {}/{} failed for workflow ID: {}. Error: {}", attempts + 1, maxAttempts, workflowId, e.getMessage(), e);
                    }
                    attempts++;
                    if (attempts < maxAttempts) {
                        Thread.sleep(delayBetweenAttempts); // Wait before retrying
                    }
                }

                // If we couldn't get a response after retries, use the default status from the workflow
                if (response == null) {
                    log.warn("Could not retrieve status for customerOrderId: {} after {} attempts. Defaulting to workflow's initial status.", customerOrderId, maxAttempts); // Updated log
                    response = parentWorkflow.getOrderStatus(); // Fallback to the workflow's default status (BLOCKED or PENDING)
                }
            } catch (Exception e) {
                log.error("Failed to start or query VehicleSalesParentWorkflow with ID: {} for customerOrderId: {}. Error: {}", workflowId, customerOrderId, e.getMessage(), e); // Updated log
                orderDetails.setOrderStatus(OrderStatus.FAILED); // Set status to FAILED instead of deleting
                orderRepository.save(orderDetails);
                failedCustomerOrderIds.add(customerOrderId);
                continue;
            }

            orderDetails.setOrderStatus(response.getOrderStatus());
            orderRepository.save(orderDetails);

            // Populate the OrderResponse with details from VehicleOrderDetails
            response = mapOrderDetailsToResponse(orderDetails, response.getOrderStatus());
            orderResponses.add(response);
        }

        if (!failedCustomerOrderIds.isEmpty()) {
            log.warn("Some orders failed to start: {}", failedCustomerOrderIds); // Updated log
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
                        .collect(Collectors.toList()), // Updated message
                orderResponses
        );

        com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                HttpStatus.ACCEPTED.value(),
                "Multiple orders placed successfully. Parent workflows started. Failed orders: " + (failedCustomerOrderIds.isEmpty() ? "None" : failedCustomerOrderIds), // Updated message
                multiOrderResponse
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    @PostMapping("/cancelOrder")
    @Operation(summary = "Cancel a vehicle order", description = "Cancels an existing vehicle order by customerOrderId") // Updated description
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order canceled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid customerOrderId"), // Updated description
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> cancelOrder(@Valid @RequestParam Long customerOrderId) {
        log.info("Canceling order with customerOrderId: {}", customerOrderId); // Updated log
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

            VehicleCancelWorkflow workflow = workflowClient.newWorkflowStub(VehicleCancelWorkflow.class, options);
            WorkflowClient.start(workflow::startCancelOrder, customerOrderId);

            OrderResponse response = workflowClient.newUntypedWorkflowStub("cancel-order-" + customerOrderId)
                    .getResult(10, TimeUnit.SECONDS, OrderResponse.class);

            log.info("Cancellation workflow started and completed successfully for customerOrderId: {}", customerOrderId); // Updated log
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Order canceled successfully with customerOrderId: " + customerOrderId, // Updated message
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to start or complete cancellation workflow for customerOrderId: {} - {}", customerOrderId, e.getMessage()); // Updated log
            OrderResponse response = vehicleOrderService.cancelOrder(customerOrderId);
            log.info("Fallback: Order canceled directly with customerOrderId: {}", customerOrderId); // Updated log
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Order canceled successfully with customerOrderId: " + customerOrderId + " (via fallback)", // Updated message
                    response
            );
            return ResponseEntity.ok(apiResponse);
        }
    }

    @GetMapping("/totalOrders")
    @Operation(summary = "Get total number of orders", description = "Returns the total number of orders in the system")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Total orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getTotalOrders() {
        try {
            long totalOrders = vehicleOrderService.getTotalOrders();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Total orders retrieved successfully",
                    totalOrders
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to retrieve total orders: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/pendingOrders")
    @Operation(summary = "Get number of pending orders", description = "Returns the number of orders with PENDING status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getPendingOrders() {
        try {
            long pendingOrders = vehicleOrderService.getPendingOrders();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Pending orders retrieved successfully",
                    pendingOrders
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to retrieve pending orders: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/financePendingOrders")
    @Operation(summary = "Get number of finance pending orders", description = "Returns the number of orders with FINANCE_PENDING status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Finance pending orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getFinancePendingOrders() {
        try {
            long financePendingOrders = vehicleOrderService.getFinancePendingOrders();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Finance pending orders retrieved successfully",
                    financePendingOrders
            );            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to retrieve finance pending orders: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal server error: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @GetMapping("/closedOrders")
    @Operation(summary = "Get number of closed orders", description = "Returns the number of orders with COMPLETED status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Closed orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse> getClosedOrders() {
        try {
            long closedOrders = vehicleOrderService.getClosedOrders();
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
                    HttpStatus.OK.value(),
                    "Closed orders retrieved successfully",
                    closedOrders
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Failed to retrieve closed orders: {}", e.getMessage());
            com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse apiResponse = new com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse(
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

    private VehicleOrderDetails mapOrderRequestToEntity(OrderRequest request) {
        VehicleOrderDetails order = new VehicleOrderDetails();
        order.setVehicleModelId(vehicleModelRepository.findById(request.getVehicleModelId())
                .orElseThrow(() -> new RuntimeException("Vehicle Model not found: " + request.getVehicleModelId())));
        order.setVehicleVariantId(vehicleVariantRepository.findById(request.getVehicleVariantId())
                .orElseThrow(() -> new RuntimeException("Vehicle Variant not found: " + request.getVehicleVariantId())));
        order.setCustomerName(request.getCustomerName());
        order.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : "");
        order.setEmail(request.getEmail() != null ? request.getEmail() : "");
        order.setPermanentAddress(request.getPermanentAddress() != null ? request.getPermanentAddress() : "");
        order.setCurrentAddress(request.getCurrentAddress() != null ? request.getCurrentAddress() : "");
        order.setAadharNo(request.getAadharNo() != null ? request.getAadharNo() : "");
        order.setPanNo(request.getPanNo() != null ? request.getPanNo() : "");
        order.setModelName(request.getModelName());
        order.setFuelType(request.getFuelType());
        order.setColour(request.getColour());
        order.setTransmissionType(request.getTransmissionType());
        order.setVariant(request.getVariant());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(BigDecimal.valueOf(request.getTotalPrice().doubleValue()));
        order.setBookingAmount(BigDecimal.valueOf(request.getBookingAmount().doubleValue()));
        order.setPaymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "");
        order.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "system");
        order.setUpdatedBy(request.getUpdatedBy() != null ? request.getUpdatedBy() : "system");
        order.setCreatedAt(LocalDateTime.now());

        return order;
    }
    @GetMapping("/orderstatus/{orderId}")
    public ResponseEntity<KendoGridResponse<OrderResponse>> getOrderStatusProgress(@PathVariable Long orderId) {
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
        order.setTotalPrice(orderDetails.getTotalPrice());
        order.setBookingAmount(orderDetails.getBookingAmount());
        order.setPaymentMode(orderDetails.getPaymentMode());
        order.setCreatedAt(orderDetails.getCreatedAt());
        order.setUpdatedAt(orderDetails.getUpdatedAt());
        order.setCreatedBy(orderDetails.getCreatedBy());
        order.setUpdatedBy(orderDetails.getUpdatedBy());
        return order;
    }

}