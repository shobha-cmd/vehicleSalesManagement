package com.vehicle.salesmanagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.controller.*;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.*;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.OrderStatus;
import com.vehicle.salesmanagement.repository.*;
import com.vehicle.salesmanagement.service.*;
import com.vehicle.salesmanagement.workflow.VehicleSalesParentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalesManagementApplicationTests {

    // Mock dependencies
    @Mock private VehicleModelService vehicleModelService;
    @Mock private VehicleOrderService vehicleOrderService;
    @Mock private FinanceService financeService;
    @Mock private DispatchDeliveryService dispatchDeliveryService;
    @Mock private VehicleOrderDetailsRepository orderRepository;
    @Mock private VehicleModelRepository vehicleModelRepository;
    @Mock private VehicleVariantRepository vehicleVariantRepository;
    @Mock private WorkflowClient workflowClient;
    @Mock private VehicleSalesParentWorkflow parentWorkflow;
    @Mock private WorkflowStub workflowStub;
    @Mock private ObjectMapper objectMapper;

    // Inject mocks into controllers
    @InjectMocks private VehicleModelController vehicleModelController;
    @InjectMocks private VehicleOrderController vehicleOrderController;
    @InjectMocks private FinanceController financeController;
    @InjectMocks private DispatchDeliveryController dispatchDeliveryController;

    // Test data objects
    private VehicleModelDTO vehicleModelDTO;
    private VehicleVariantDTO vehicleVariantDTO;
    private StockDetailsDTO stockDetailsDTO;
    private MddpStockDTO mddpStockDTO;
    private ManufacturerOrderDTO manufacturerOrderDTO;
    private OrderRequest orderRequest;
    private MultiOrderRequest multiOrderRequest;
    private FinanceRequest financeRequest;
    private ApproveFinanceRequest approveFinanceRequest;
    private RejectFinanceRequest rejectFinanceRequest;
    private DispatchRequest dispatchRequest;
    private DeliveryRequest deliveryRequest;
    private VehicleOrderDetails vehicleOrderDetails;
    private VehicleModel vehicleModel;
    private VehicleVariant vehicleVariant;
    private OrderResponse orderResponse;
    private FinanceResponse financeResponse;
    private DispatchResponse dispatchResponse;
    private DeliveryResponse deliveryResponse;

    private LocalDateTime fixedTimestamp;
    private MockedStatic<LocalDateTime> localDateTimeMock;

    @BeforeEach
    void setUp() {
        // Mock LocalDateTime.now() to return a fixed timestamp
        fixedTimestamp = ZonedDateTime.of(2025, 6, 4, 10, 6, 0, 0, ZoneId.of("Asia/Kolkata"))
                .toLocalDateTime();
        localDateTimeMock = mockStatic(LocalDateTime.class);
        localDateTimeMock.when(LocalDateTime::now).thenReturn(fixedTimestamp);

        // Initialize test data objects
        initializeTestData();
    }

    @AfterEach
    void tearDown() {
        localDateTimeMock.close();
    }

    private void initializeTestData() {
        // Vehicle Model DTO
        vehicleModelDTO = new VehicleModelDTO();
        vehicleModelDTO.setModelName("TestModel");
        
        // Vehicle Variant DTO
        vehicleVariantDTO = new VehicleVariantDTO();
        vehicleVariantDTO.setVehicleModelId(1L);
        vehicleVariantDTO.setVariant("TestVariant");
        
        // Stock Details DTO
        stockDetailsDTO = new StockDetailsDTO();
        stockDetailsDTO.setVehicleModelId(1L);
        stockDetailsDTO.setVehicleVariantId(1L);
        
        // MDDP Stock DTO
        mddpStockDTO = new MddpStockDTO();
        mddpStockDTO.setMddpId(1);
        
        // Manufacturer Order DTO
        manufacturerOrderDTO = new ManufacturerOrderDTO();
        manufacturerOrderDTO.setManufacturerId(1L);
        
        // Order Request
        orderRequest = new OrderRequest();
        orderRequest.setCustomerName("John Doe");
        orderRequest.setVehicleModelId(1L);
        orderRequest.setVehicleVariantId(1L);
        
        // Multi Order Request
        multiOrderRequest = new MultiOrderRequest();
        multiOrderRequest.setVehicleOrders(List.of(orderRequest));
        
        // Finance Request
        financeRequest = new FinanceRequest();
        financeRequest.setCustomerOrderId(String.valueOf(1L));
        
        // Approve Finance Request
        approveFinanceRequest = new ApproveFinanceRequest();
        approveFinanceRequest.setCustomerOrderId(String.valueOf(1L));
        
        // Reject Finance Request
        rejectFinanceRequest = new RejectFinanceRequest();
        rejectFinanceRequest.setCustomerOrderId(String.valueOf(1L));
        
        // Dispatch Request
        dispatchRequest = new DispatchRequest();
        dispatchRequest.setCustomerOrderId(String.valueOf(1L));
        
        // Delivery Request
        deliveryRequest = new DeliveryRequest();
        deliveryRequest.setCustomerOrderId(String.valueOf(1L));
        
        // Vehicle Model Entity
        vehicleModel = new VehicleModel();
        vehicleModel.setVehicleModelId(1L);
        
        // Vehicle Variant Entity
        vehicleVariant = new VehicleVariant();
        vehicleVariant.setVehicleVariantId(1L);
        
        // Vehicle Order Details
        vehicleOrderDetails = new VehicleOrderDetails();
        vehicleOrderDetails.setCustomerOrderId(String.valueOf(1L));
        
        // Order Response
        orderResponse = new OrderResponse();
        orderResponse.setCustomerOrderId(String.valueOf(1L));
        
        // Finance Response
        financeResponse = new FinanceResponse();
        financeResponse.setFinanceId(1L);
        
        // Dispatch Response
        dispatchResponse = new DispatchResponse();
        dispatchResponse.setDispatchId(1L);
        
        // Delivery Response
        deliveryResponse = new DeliveryResponse();
        deliveryResponse.setDeliveryId(1L);
    }

    @Test
    void contextLoads() {
        assertNotNull(vehicleModelController);
        assertNotNull(vehicleOrderController);
        assertNotNull(financeController);
        assertNotNull(dispatchDeliveryController);
    }

    // Vehicle Model Controller Tests
    @Test
    void saveVehicleModels_Success() {
        when(vehicleModelService.saveVehicleModels(anyList()))
            .thenReturn(new KendoGridResponse<>(List.of(vehicleModel), 1L, null, null));
        
        ResponseEntity<KendoGridResponse<VehicleModel>> response = 
            vehicleModelController.saveVehicleModels(List.of(vehicleModelDTO));
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotal());
    }

    @Test
    void getDropdownData_Success() {
        VehicleAttributesResponse mockResponse = new VehicleAttributesResponse();
        when(vehicleModelService.getDropdownData(any(), any(), any(), any()))
            .thenReturn(mockResponse);
        
        ResponseEntity<ApiResponse> response = 
            vehicleModelController.getDropdownData(null, null, null, null);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockResponse, response.getBody().getData());
    }

    // Vehicle Order Controller Tests
    @Test
    void placeOrder_SingleOrder_Success() throws Exception {
        when(orderRepository.saveAndFlush(any())).thenReturn(vehicleOrderDetails);
        when(vehicleModelRepository.findById(anyLong())).thenReturn(Optional.of(vehicleModel));
        when(vehicleVariantRepository.findById(anyLong())).thenReturn(Optional.of(vehicleVariant));
        when(workflowClient.newWorkflowStub((Class<Object>) any(), (WorkflowOptions) any())).thenReturn(parentWorkflow);
        
        ResponseEntity<ApiResponse> response = 
            vehicleOrderController.placeOrder(orderRequest);
        
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(response.getBody().getStatusMessage().contains("Order placed successfully"));
    }

    @Test
    void cancelOrder_Success() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(vehicleOrderDetails));
        
        ResponseEntity<ApiResponse> response = 
            vehicleOrderController.cancelOrder(String.valueOf(1L));
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getStatusMessage().contains("Order canceled successfully"));
    }

    // Finance Controller Tests
    @Test
    void initiateFinance_Success() {
        vehicleOrderDetails.setOrderStatus(OrderStatus.BLOCKED);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(vehicleOrderDetails));
        when(financeService.createFinanceDetails(any())).thenReturn(financeResponse);
        when(workflowClient.newUntypedWorkflowStub(anyString())).thenReturn(workflowStub);
        
        ResponseEntity<ApiResponse<FinanceResponse>> response = 
            financeController.initiateFinance(financeRequest);
        
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void approveFinance_Success() {
        when(workflowClient.newUntypedWorkflowStub(anyString())).thenReturn(workflowStub);
        when(workflowStub.getResult(any())).thenReturn(financeResponse);
        
        ResponseEntity<ApiResponse<FinanceResponse>> response = 
            financeController.approveFinance(approveFinanceRequest);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getData());
    }

    // Dispatch Delivery Controller Tests
    @Test
    void initiateDispatch_Success() {
        when(dispatchDeliveryService.initiateDispatch(any())).thenReturn(dispatchResponse);
        when(workflowClient.newUntypedWorkflowStub(anyString())).thenReturn(workflowStub);
        
        ApiResponse<DispatchResponse> response = 
            dispatchDeliveryController.initiateDispatch(dispatchRequest);
        
        assertEquals(HttpStatus.ACCEPTED.value(), response.getStatusCode());
        assertNotNull(response.getData());
    }

    @Test
    void confirmDelivery_Success() {
        when(dispatchDeliveryService.confirmDelivery(any())).thenReturn(deliveryResponse);
        when(workflowClient.newUntypedWorkflowStub(anyString())).thenReturn(workflowStub);
        
        ApiResponse<DeliveryResponse> response = 
            dispatchDeliveryController.confirmDelivery(deliveryRequest);
        
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertNotNull(response.getData());
    }

    // Additional test cases for error scenarios
    @Test
    void placeOrder_ValidationFailure() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new IllegalArgumentException("Invalid JSON"));
        
        ResponseEntity<ApiResponse> response = 
            vehicleOrderController.placeOrder(orderRequest);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getStatusMessage().contains("Invalid request"));
    }

    @Test
    void initiateFinance_OrderNotBlocked() {
        vehicleOrderDetails.setOrderStatus(OrderStatus.PENDING);
        when(orderRepository.findById(anyLong())).thenReturn(Optional.of(vehicleOrderDetails));
        
        ResponseEntity<ApiResponse<FinanceResponse>> response = 
            financeController.initiateFinance(financeRequest);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getStatusMessage().contains("BLOCKED status"));
    }

    @Test
    void initiateDispatch_ServiceError() {
        when(dispatchDeliveryService.initiateDispatch(any()))
            .thenThrow(new IllegalStateException("Order must be ALLOTTED"));
        
        ApiResponse<DispatchResponse> response = 
            dispatchDeliveryController.initiateDispatch(dispatchRequest);
        
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
        assertTrue(response.getStatusMessage().contains("ALLOTTED"));
    }
}