package com.vehicle.salesmanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.domain.dto.apirequest.ApproveFinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.FinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apirequest.RejectFinanceRequest;
import com.vehicle.salesmanagement.domain.dto.apiresponse.ApiResponse;
import com.vehicle.salesmanagement.domain.dto.apiresponse.FinanceResponse;
import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.VehicleOrderDetailsRepository;
import com.vehicle.salesmanagement.service.FinanceService;
import com.vehicle.salesmanagement.workflow.VehicleSalesParentWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
@Tag(name = "Finance Management")
public class FinanceController {

    private final @Qualifier("financeWorkflowClient") WorkflowClient workflowClient;
    private final FinanceService financeService;
    private final VehicleOrderDetailsRepository vehicleOrderDetailsRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/financeInitiate")
    @Operation(summary = "Initiate finance details", description = "Creates finance details for a vehicle order and signals the parent workflow to start the finance workflow")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Finance workflow started successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or order not in BLOCKED status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<FinanceResponse>> initiateFinance(@Valid @RequestBody FinanceRequest financeRequest) {
        log.info("Creating finance details for order ID: {}", financeRequest.getCustomerOrderId());

        VehicleOrderDetails orderDetails = vehicleOrderDetailsRepository.findByCustomerOrderId(financeRequest.getCustomerOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + financeRequest.getCustomerOrderId()));
        if (orderDetails.getOrderStatus() != OrderStatus.BLOCKED) {
            log.error("Order ID: {} is not in BLOCKED status, current status: {}", financeRequest.getCustomerOrderId(), orderDetails.getOrderStatus());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Finance initiation failed: Order must be in BLOCKED status",
                    null
            ));
        }

        FinanceResponse financeResponse;
        try {
            financeResponse = financeService.createFinanceDetails(financeRequest);
        } catch (RuntimeException e) {
            log.error("Failed to create finance details for order ID: {}: {}", financeRequest.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Failed to create finance details: " + e.getMessage(),
                    null
            ));
        }

        // Signal the parent workflow to initiate finance
        String parentWorkflowId = "parent-" + financeRequest.getCustomerOrderId();
        log.info("Attempting to signal parent workflow with ID: {} for order ID: {}", parentWorkflowId, financeRequest.getCustomerOrderId());

        // Check if the parent workflow is running
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(
                parentWorkflowId
        );
        try {
            String status = stub.query("getWorkflowStatus", String.class);
            log.info("Parent workflow with ID: {} is running with status: {}", parentWorkflowId, status);
        } catch (Exception e) {
            log.error("Parent workflow with ID: {} is not running for order ID: {}. Error: {}",
                    parentWorkflowId, financeRequest.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Cannot initiate finance: Parent workflow is not running for order ID: " + financeRequest.getCustomerOrderId(),
                    null
            ));
        }

        // If the workflow is running, proceed to signal
        try {
            VehicleSalesParentWorkflow parentWorkflow = workflowClient.newWorkflowStub(
                    VehicleSalesParentWorkflow.class,
                    parentWorkflowId,
                    Optional.of("vehicle-order-task-queue")
            );
            parentWorkflow.initiateFinance(financeRequest);
            log.info("Signaled parent workflow to initiate finance for order ID: {}", financeRequest.getCustomerOrderId());
        } catch (Exception e) {
            log.error("Failed to signal parent workflow for order ID: {}, workflowId: {}: {}",
                    financeRequest.getCustomerOrderId(), parentWorkflowId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to signal parent workflow to initiate finance: " + e.getMessage(),
                    null
            ));
        }

        try {
            String financeResponseJson = objectMapper.writeValueAsString(financeResponse);
            log.info("Finance details created successfully for order ID: {}, stored finance details: {}",
                    financeRequest.getCustomerOrderId(), financeResponseJson);
        } catch (Exception e) {
            log.error("Failed to serialize finance details for logging: {}", e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(
                HttpStatus.ACCEPTED.value(),
                "Finance details created successfully",
                financeResponse
        ));
    }

    @PostMapping("/financeApprove")
    @Operation(summary = "Approve finance for an order", description = "Approves the finance workflow for a vehicle order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Finance approved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<FinanceResponse>> approveFinance(@Valid @RequestBody ApproveFinanceRequest request) {
        log.info("Approving finance for order ID: {}", request.getCustomerOrderId());
        String workflowId = "finance-" + request.getCustomerOrderId();

        try {
            // Use an untyped workflow stub to signal the existing workflow
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(workflowId);
            workflow.signal("approveFinance", request.getApprovedBy());

            // Wait for the workflow to complete and get the result
            FinanceResponse financeResponse = workflow.getResult(FinanceResponse.class);
            log.info("Finance approved, order status set to ALLOTTED for order ID: {}", request.getCustomerOrderId());

            return ResponseEntity.ok(new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Finance approved successfully",
                    financeResponse
            ));
        } catch (WorkflowNotFoundException e) {
            log.error("Finance workflow not found for order ID: {}, workflowId: {}", request.getCustomerOrderId(), workflowId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Finance workflow not found for workflowId='" + workflowId + "'",
                    null
            ));
        } catch (WorkflowFailedException e) {
            log.error("Workflow failed for order ID: {}, workflowId: {}: {}", request.getCustomerOrderId(), workflowId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to approve finance: Workflow execution failed for workflowId='" + workflowId + "': " + e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to approve finance for order ID: {}, workflowId: {}: {}", request.getCustomerOrderId(), workflowId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to approve finance for workflowId='" + workflowId + "': " + e.getMessage(),
                    null
            ));
        }
    }

    @PostMapping("/financeReject")
    @Operation(summary = "Reject finance for an order", description = "Rejects the finance workflow for a vehicle order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Finance rejected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<FinanceResponse>> rejectFinance(@Valid @RequestBody RejectFinanceRequest request) {
        log.info("Rejecting finance for order ID: {}", request.getCustomerOrderId());
        String workflowId = "finance-" + request.getCustomerOrderId();

        try {
            WorkflowStub workflow = workflowClient.newUntypedWorkflowStub(workflowId);
            workflow.signal("rejectFinance", request.getRejectedBy());

            FinanceResponse financeResponse = workflow.getResult(FinanceResponse.class);
            log.info("Finance rejected, order status set to PENDING for order ID: {}", request.getCustomerOrderId());

            return ResponseEntity.ok(new ApiResponse<>(
                    HttpStatus.OK.value(),
                    "Finance rejected successfully",
                    financeResponse
            ));
        } catch (WorkflowNotFoundException e) {
            log.error("Finance workflow not found for order ID: {}, workflowId: {}", request.getCustomerOrderId(), workflowId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(
                    HttpStatus.NOT_FOUND.value(),
                    "Finance workflow not found for workflowId='" + workflowId + "'",
                    null
            ));
        } catch (WorkflowFailedException e) {
            log.error("Workflow failed for order ID: {}: {}", request.getCustomerOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reject finance: Workflow execution failed for workflowId='" + workflowId + "': " + e.getMessage(),
                    null
            ));
        } catch (Exception e) {
            log.error("Failed to reject finance for order ID: {}, workflowId: {}: {}", request.getCustomerOrderId(), workflowId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to reject finance for workflowId='" + workflowId + "': " + e.getMessage(),
                    null
            ));
        }
    }
}