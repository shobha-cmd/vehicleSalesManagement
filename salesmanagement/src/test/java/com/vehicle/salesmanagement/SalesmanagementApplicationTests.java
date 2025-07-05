package com.vehicle.salesmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vehicle.salesmanagement.controller.VehicleOrderController;
import com.vehicle.salesmanagement.domain.dto.apirequest.*;
import com.vehicle.salesmanagement.domain.dto.apiresponse.*;
import com.vehicle.salesmanagement.domain.entity.model.*;
import com.vehicle.salesmanagement.enums.*;
import com.vehicle.salesmanagement.repository.*;
import com.vehicle.salesmanagement.service.*;
import com.vehicle.salesmanagement.workflow.VehicleSalesParentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.RequestEntity.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SalesmanagementApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DispatchDeliveryService dispatchDeliveryService;

    @MockBean
    private FinanceService financeService;

    @MockBean
    @Qualifier("financeWorkflowClient")
    private WorkflowClient workflowClient;

    @MockBean
    private WorkerFactory workerFactory;

    @MockBean
    private WorkflowStub workflowStub;

    @MockBean
    private VehicleSalesParentWorkflow vehicleSalesParentWorkflow;

    @MockBean
    private VehicleModelService vehicleModelService;

    @SpyBean
    private VehicleOrderService vehicleOrderService;

    @MockBean
    private VehicleOrderDetailsRepository vehicleOrderDetailsRepository;

    @MockBean
    private VehicleModelRepository vehicleModelRepository;

    @MockBean
    private VehicleVariantRepository vehicleVariantRepository;

    @MockBean
    private ManufacturerOrderRepository manufacturerOrderRepository;
    @MockBean
    private StockDetailsRepository stockDetailsRepository;

    @MockBean
    private OrderIdGeneratorService orderIdGeneratorService;

    @MockBean
    private HistoryService historyService;

    private VehicleOrderDetails orderDetails;
    private VehicleModel vehicleModel;
    private VehicleVariant vehicleVariant;
    private DispatchRequest dispatchRequest;
    private DeliveryRequest deliveryRequest;
    private DispatchResponse dispatchResponse;
    private DeliveryResponse deliveryResponse;
    private FinanceRequest financeRequest;
    private FinanceResponse financeResponse;



    @Autowired
    private VehicleOrderController vehicleOrderController;


    @BeforeEach
    void setUp() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2025, 6, 19, 12, 45, 0, 0, ZoneId.of("Asia/Kolkata"));
        LocalDateTime timestamp = zonedDateTime.toLocalDateTime();

        vehicleModel = new VehicleModel();
        vehicleModel.setVehicleModelId(1L);
        vehicleModel.setModelName("Test Model");

        vehicleVariant = new VehicleVariant();
        vehicleVariant.setVehicleVariantId(1L);
        vehicleVariant.setVehicleModelId(vehicleModel);
        vehicleVariant.setVariant("Test Variant");

        orderDetails = new VehicleOrderDetails();
        orderDetails.setCustomerOrderId("123");
        orderDetails.setCustomerName("Test Customer");
        orderDetails.setOrderStatus(OrderStatus.BLOCKED);
        orderDetails.setModelName("Test Model");
        orderDetails.setVariant("Test Variant");
        orderDetails.setQuantity(1);
        orderDetails.setColour("Red");
        orderDetails.setFuelType("Petrol");
        orderDetails.setTransmissionType("Manual");
        orderDetails.setVehicleModelId(vehicleModel);
        orderDetails.setVehicleVariantId(vehicleVariant);
        //orderDetails.setCreatedAt(timestamp);

        dispatchRequest = new DispatchRequest();
        dispatchRequest.setCustomerOrderId("123");
        dispatchRequest.setDispatchedBy("Test Dispatcher");

        deliveryRequest = new DeliveryRequest();
        deliveryRequest.setCustomerOrderId("123");
        deliveryRequest.setDeliveredBy("Test Deliverer");
        deliveryRequest.setRecipientName("Test Recipient");

        financeRequest = new FinanceRequest();
        financeRequest.setCustomerOrderId("123");
        financeRequest.setCustomerName("Test Customer");
        financeRequest.setVehicleModelId(1L);
        financeRequest.setVehicleVariantId(1L);
        financeRequest.setModelName("Test Model");
        financeRequest.setVariant("Test Variant");
        financeRequest.setColour("Red");
        financeRequest.setFuelType("Petrol");
        financeRequest.setTransmissionType("Manual");
        financeRequest.setQuantity(1);
        financeRequest.setPaymentMode("Cash");

        financeResponse = new FinanceResponse();
        financeResponse.setCustomerOrderId("123");
        financeResponse.setCustomerName("Test Customer");
        financeResponse.setOrderStatus(OrderStatus.BLOCKED);
        financeResponse.setModelName("Test Model");
        financeResponse.setVariant("Test Variant");


        dispatchResponse = new DispatchResponse();
        dispatchResponse.setCustomerOrderId("123");
        dispatchResponse.setDispatchStatus(DispatchStatus.PREPARING);
        dispatchResponse.setOrderStatus(OrderStatus.DISPATCHED);

        deliveryResponse = new DeliveryResponse();
        deliveryResponse.setCustomerOrderId("123");
        deliveryResponse.setDeliveryStatus(DeliveryStatus.DELIVERED);
        deliveryResponse.setOrderStatus(OrderStatus.DELIVERED);
    }

    // VehicleModelController Tests
    @Test
    void testSaveVehicleModels_Success() throws Exception {
        VehicleModelDTO dto = new VehicleModelDTO();
        dto.setModelName("Test Model");

        VehicleModel model = new VehicleModel();
        model.setModelName("Test Model");

        when(vehicleModelService.saveVehicleModels(anyList())).thenReturn(new KendoGridResponse<>(List.of(model), 1, null, null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/vehiclemodels/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].modelName").value(is("Test Model")));
    }

    @Test
    void testSaveVehicleVariants_Success() throws Exception {
        VehicleVariantDTO dto = new VehicleVariantDTO();
        dto.setModelName("Test Model");
        dto.setVariant("Test Variant");
        dto.setVehicleModelId(1L);

        when(vehicleModelService.saveVehicleVariants(anyList()))
                .thenReturn(new KendoGridResponse<>(List.of(vehicleVariant), 1, null, null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/vehiclevariants/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].variant").value(is("Test Variant")));
    }

    @Test
    void testSaveStockDetails_Success() throws Exception {
        StockDetailsDTO dto = new StockDetailsDTO();
        dto.setModelName("Test Model");
        dto.setVehicleModelId(1L);
        dto.setVehicleVariantId(1L);
        dto.setQuantity(10);

        StockDetails stock = new StockDetails();
        stock.setModelName("Test Model");
        stock.setQuantity(10);

        when(vehicleModelService.saveStockDetails(anyList()))
                .thenReturn(new KendoGridResponse<>(List.of(stock), 1, null, null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/stockdetails/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].modelName").value(is("Test Model")));
    }

    @Test
    void testSaveMddpStock_Success() throws Exception {
        MddpStockDTO dto = new MddpStockDTO();
        dto.setModelName("Test Model");
        dto.setVehicleModelId(1L);
        dto.setVehicleVariantId(1L);
        dto.setQuantity(5);

        MddpStock stock = new MddpStock();
        stock.setVehicleModelId(vehicleModel);
        stock.setQuantity(5);

        when(vehicleModelService.saveMddpStock(anyList()))
                .thenReturn(new KendoGridResponse<>(List.of(stock), 1, null, null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/mddpstock/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].vehicleModelId.modelName").value(is("Test Model")));
    }

    @Test
    void testSaveManufacturerOrders_Success() throws Exception {
        ManufacturerOrderDTO dto = new ManufacturerOrderDTO();
        dto.setModelName("Test Model");
        dto.setVehicleVariantId(1L);

        ManufacturerOrder order = new ManufacturerOrder();
        order.setModelName("Test Model");

        when(vehicleModelService.saveManufacturerOrders(anyList()))
                .thenReturn(new KendoGridResponse<>(List.of(order), 1, null, null));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/manufacturerorders/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].modelName").value(is("Test Model")));
    }

    @Test
    void testGetDropdownData_Success() throws Exception {
        // Given
        VehicleAttributesResponse response = new VehicleAttributesResponse();
        response.setModelNames(Collections.singletonList("Test Model"));

        // Setup variant
        VehicleAttributesResponse.Variant variant = new VehicleAttributesResponse.Variant("Test Variant", 1L);

        // Setup modelAttributes
        VehicleAttributesResponse.ModelAttributes modelAttributes = new VehicleAttributesResponse.ModelAttributes();
        modelAttributes.setVariants(Collections.singletonList(variant));

        // Set modelDetails
        Map<String, VehicleAttributesResponse.ModelAttributes> modelDetails = new HashMap<>();
        modelDetails.put("Test Model", modelAttributes);
        response.setModelDetails(modelDetails);

        // Mocking the service response
        when(vehicleModelService.getDropdownData(anyString(), anyString(), any(), any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/dropdownData")
                        .param("modelName", "Test Model")
                        .param("variant", "Test Variant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modelNames[0]").value("Test Model"))
                .andExpect(jsonPath("$.data[0].modelDetails['Test Model'].variants[0].name").value("Test Variant"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.aggregateResults").isString());
    }



    @Test
    void testGetAllStockDetails_Success() throws Exception {
        StockDetailsDTO dto = new StockDetailsDTO();
        dto.setModelName("Test Model");
        dto.setQuantity(10);
        dto.setVariant("Test Variant");
        dto.setStockId(1L);
        dto.setVehicleModelId(1L);
        dto.setVehicleVariantId(1L);

        when(vehicleModelService.getAllStockDetails())
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/stockdetails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"))
                .andExpect(jsonPath("$.data[0].variant").value("Test Variant"));
    }

    @Test
    void testGetAllMddpStock_Success() throws Exception {
        MddpStockDTO dto = new MddpStockDTO();
        dto.setModelName("Test Model");
        dto.setQuantity(5);

        when(vehicleModelService.getAllMddpStock())
                .thenReturn(new KendoGridResponse<>(List.of(dto), 1, null, null));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/mddpstock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"));
    }

    @Test
    void testGetAllVehicleVariants_Success() throws Exception {
        when(vehicleModelService.getAllVehicleVariants())
                .thenReturn(new KendoGridResponse<>(List.of(vehicleVariant), 1, null, null));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vehiclevariants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].variant").value(is("Test Variant")));
    }

    @Test
    void testGetAllManufacturerOrders_Success() throws Exception {
        ManufacturerOrderDTO orderDTO = new ManufacturerOrderDTO();
        orderDTO.setModelName("Test Model");  // set the expected value

        KendoGridResponse<ManufacturerOrderDTO> response = new KendoGridResponse<>();
        response.setData(Collections.singletonList(orderDTO));
        response.setTotal(1);

        when(vehicleModelService.getAllManufacturerOrders()).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/manufacturerorders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"));
    }


    @Test
    void testGetStockDetailByModelAndVariant_Success() throws Exception {
        StockDetailsDTO stockDTO = new StockDetailsDTO();
        stockDTO.setStockId(1L);
        stockDTO.setModelName("Test Model");
        stockDTO.setVariant("Test Variant");
        stockDTO.setQuantity(10);
        stockDTO.setVehicleModelId(1L);
        stockDTO.setVehicleVariantId(1L);

        when(vehicleModelService.getStockDetailByModelAndVariant(anyString(), anyLong()))
                .thenReturn(stockDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/stockdetails/find")
                        .param("modelName", "Test Model")
                        .param("vehicleVariantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"))
                .andExpect(jsonPath("$.data[0].variant").value("Test Variant"));
    }

    @Test
    void testGetStockDetailByModelAndVariant_NotFound() throws Exception {
        when(vehicleModelService.getStockDetailByModelAndVariant(anyString(), anyLong()))
                .thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/stockdetails/find")
                        .param("modelName", "NonExistentModel")
                        .param("vehicleVariantId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetMddpStockByModelAndVariant_Success() throws Exception {
        MddpStockDTO stockDTO = new MddpStockDTO();
        stockDTO.setModelName("Test Model");
        stockDTO.setVariant("Test Variant");
        stockDTO.setVehicleModelId(1L);
        stockDTO.setVehicleVariantId(1L);
        stockDTO.setColour("Red");
        stockDTO.setFuelType("Petrol");
        stockDTO.setTransmissionType("Automatic");
        stockDTO.setQuantity(1);
        stockDTO.setStockStatus("AVAILABLE");
        stockDTO.setExpectedDispatchDate(LocalDateTime.now());
        stockDTO.setExpectedDeliveryDate(LocalDateTime.now().plusDays(7));

        when(vehicleModelService.getMddpStockByModelAndVariant(anyString(), anyLong()))
                .thenReturn(stockDTO);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/mddpstock/find")
                        .param("modelName", "Test Model")
                        .param("vehicleVariantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"))
                .andExpect(jsonPath("$.data[0].variant").value("Test Variant"));

    }

    @Test
    void testGetMddpStockByModelAndVariant_NotFound() throws Exception {
        when(vehicleModelService.getMddpStockByModelAndVariant(anyString(), anyLong()))
                .thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/mddpstock/find")
                        .param("modelName", "NonExistentModel")
                        .param("vehicleVariantId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetVehicleVariantByModelAndVariant_Success() throws Exception {
        VehicleVariantDTO variantDTO = new VehicleVariantDTO();
        variantDTO.setVariant("Test Variant");
        variantDTO.setModelName("Test Model");
        variantDTO.setVehicleModelId(1L);
        variantDTO.setColour("Red");
        variantDTO.setFuelType("Petrol");
        variantDTO.setTransmissionType("Automatic");
        variantDTO.setPrice(BigDecimal.valueOf(100000));
        variantDTO.setYearOfManufacture(2023);

        when(vehicleModelService.getVehicleVariantByModelAndVariant(anyString(), anyLong()))
                .thenReturn(variantDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vehiclevariants/find")
                        .param("modelName", "Test Model")
                        .param("vehicleVariantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].variant").value("Test Variant"))
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"));
    }


    @Test
    void testGetVehicleVariantByModelAndVariant_NotFound() throws Exception {
        when(vehicleModelService.getVehicleVariantByModelAndVariant(anyString(), anyLong()))
                .thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vehiclevariants/find")
                        .param("modelName", "NonExistentModel")
                        .param("vehicleVariantId", "999"))
                .andExpect(status().isNotFound());
    }



    @Test
    void testUpdateStockDetails_Success() throws Exception {
        StockDetailsDTO dto = new StockDetailsDTO();
        dto.setStockId(1L);
        dto.setVehicleModelId(1L);
        dto.setVehicleVariantId(1L);
        dto.setModelName("Test Model");
        dto.setVariant("Test Variant");
        dto.setColour("Red");
        dto.setEngineColour("Black");
        dto.setInteriorColour("Beige");
        dto.setFuelType("Petrol");
        dto.setTransmissionType("Automatic");
        dto.setQuantity(10);
        //dto.setVinNumber("TESTVIN1234567890");
        dto.setStockStatus("AVAILABLE");
        dto.setSuffix("Test");

        StockDetails stock = new StockDetails();
        stock.setStockId(1L);
        stock.setModelName("Test Model");
        stock.setVehicleModelId(vehicleModel);
        stock.setVehicleVariantId(vehicleVariant);
        stock.setVariant("Test Variant");

        KendoGridResponse<StockDetails> response = new KendoGridResponse<>(List.of(stock), 1L, null, null);
        when(vehicleModelService.updateStockDetails(anyList())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/stockdetails/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"))
                .andExpect(jsonPath("$.data[0].variant").value("Test Variant"));
    }

    @Test
    void testUpdateMddpStock_Success() throws Exception {
        MddpStockDTO dto = new MddpStockDTO();
        dto.setModelName("Test Model");
        dto.setVariant("Test Variant");
        dto.setColour("Red");
        dto.setEngineColour("Black");
        dto.setFuelType("Petrol");
        dto.setTransmissionType("Automatic");
        dto.setQuantity(5);
        dto.setStockStatus("AVAILABLE");

        MddpStock stock = new MddpStock();
        stock.setMddpId(1L); // Use 1L to specify a Long literal
        stock.setModelName("Test Model");
        stock.setVariant("Test Variant");
        stock.setVehicleModelId(vehicleModel);

        KendoGridResponse<MddpStock> response = new KendoGridResponse<>(List.of(stock), 1L, null, null);
        when(vehicleModelService.updateMddpStock(anyList())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/mddpstock/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].modelName").value("Test Model"))
                .andExpect(jsonPath("$.data[0].variant").value("Test Variant"));
    }

    @Test
    void testUpdateVehicleVariants_Success() throws Exception {
        VehicleVariantDTO dto = new VehicleVariantDTO();
        dto.setVehicleModelId(1L);
        dto.setVariant("Test Variant");

        KendoGridResponse<VehicleVariant> response = new KendoGridResponse<>(
                List.of(vehicleVariant),
                1L,
                null,
                null
        );

        when(vehicleModelService.updateVehicleVariants(anyList())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/vehiclevariants/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].variant").value(is("Test Variant")));
    }

    @Test
    void testUpdateManufacturerOrders_Success() throws Exception {
        ManufacturerOrderDTO dto = new ManufacturerOrderDTO();
        dto.setManufacturerId(1L);
        dto.setModelName("Test Model");

        ManufacturerOrder order = new ManufacturerOrder();
        order.setManufacturerId(1L);
        order.setModelName("Test Model");

        KendoGridResponse<ManufacturerOrder> response = new KendoGridResponse<>();
        response.setData(Collections.singletonList(order));
        response.setTotal(1);

        when(vehicleModelService.updateManufacturerOrders(anyList())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/manufacturerorders/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].modelName").value(is("Test Model")));
    }

    // VehicleOrderController Tests
    @Test
    void testPlaceSingleOrder_Success() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setCustomerOrderId("ORDER-123456");
        request.setVehicleModelId(1L);
        request.setVehicleVariantId(1L);
        request.setCustomerName("Test Customer");
        request.setPhoneNumber("1234567890");
        request.setEmail("test.customer@example.com");
        request.setAadharNo("123456789012");
        request.setPanNo("ABCDE1234F");
        request.setModelName("Test Model");
        request.setFuelType("Petrol");
        request.setColour("Red");
        request.setTransmissionType("Manual");
        request.setVariant("Test Variant");
        request.setQuantity(1);
        request.setPaymentMode("CASH");
        request.setExpectedDeliveryDate("2025-07-05"); // Set to tomorrow

        mockMvc.perform(MockMvcRequestBuilders.post("/api/placeOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500));
    }
    @Test
    void testPlaceSingleOrder_ModelNotFound() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Test Customer");
        request.setVehicleModelId(1L);
        request.setVehicleVariantId(1L);
        request.setModelName("Test Model");
        request.setVariant("Test Variant");
        request.setColour("Red");
        request.setFuelType("Petrol");
        request.setTransmissionType("Manual");
        request.setQuantity(1);
        request.setPhoneNumber("1234567890");
        request.setAadharNo("123456789012");
        request.setEmail("test.customer@example.com");
        request.setPanNo("ABCDE1234F");
        request.setPaymentMode("CASH");
        request.setExpectedDeliveryDate("2025-07-05"); // Set to tomorrow to pass @Future validation

        // Mock repository to simulate model not found
        when(vehicleModelRepository.existsById(1L)).thenReturn(true); // Pass initial validation
        when(vehicleModelRepository.findById(1L)).thenReturn(Optional.empty()); // Fail in mapOrderRequestToEntity
        when(vehicleVariantRepository.existsById(1L)).thenReturn(true); // Pass variant validation
        when(vehicleVariantRepository.findById(1L)).thenReturn(Optional.of(new VehicleVariant())); // Provide valid variant

        mockMvc.perform(MockMvcRequestBuilders.post("/api/placeOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.statusMessage").value(containsString("Vehicle Model with ID 1 not found")));
    }



//    @Test
//    void testPlaceMultiOrder_Success() throws Exception {
//        // 1. Prepare the order request
//        OrderRequest order1 = new OrderRequest();
//        order1.setCustomerName("Test Customer");
//        order1.setVehicleModelId(1L);
//        order1.setVehicleVariantId(1L);
//        order1.setModelName("Test Model");
//        order1.setVariant("Test Variant");
//        order1.setColour("Red");
//        order1.setFuelType("Petrol");
//        order1.setTransmissionType("Manual");
//        order1.setQuantity(1);
//        order1.setPhoneNumber("1234567890");
//        order1.setAadharNo("123456789012");
//        order1.setEmail("test.customer@example.com");
//        order1.setPanNo("ABCDE1234F");
//        order1.setPaymentMode("CASH");
//
//        MultiOrderRequest multiOrderRequest = new MultiOrderRequest();
//        multiOrderRequest.setVehicleOrders(Collections.singletonList(order1));
//
//        // 2. Mock required entities
//        VehicleModel model = new VehicleModel();
//        model.setVehicleModelId(1L);
//
//        VehicleVariant variant = new VehicleVariant();
//        variant.setVehicleVariantId(1L);
//
//        StockDetails stock = new StockDetails();
//        stock.setStockId(1L);
//        stock.setVehicleModelId(model);
//        stock.setVehicleVariantId(variant);
//        stock.setModelName("Test Model");
//        stock.setVariant("Test Variant");
//        stock.setColour("Red");
//        stock.setFuelType("Petrol");
//        stock.setTransmissionType("Manual");
//        stock.setQuantity(10); // Sufficient stock
//        stock.setStockStatus(StockStatus.AVAILABLE);
//
//        // 3. Mock repository and service behavior
//        when(vehicleModelRepository.existsById(1L)).thenReturn(true);
//        when(vehicleVariantRepository.existsById(1L)).thenReturn(true);
//        when(vehicleModelRepository.findById(1L)).thenReturn(Optional.of(model));
//        when(vehicleVariantRepository.findById(1L)).thenReturn(Optional.of(variant));
//
//        when(orderIdGeneratorService.generateCustomerOrderId()).thenReturn("ORDER-123456");
//
//        when(stockDetailsRepository.findByModelNameAndVehicleVariantIdAndStockStatus(
//                eq("Test Model"), eq(variant), eq(StockStatus.AVAILABLE)))
//                .thenReturn(Collections.singletonList(stock));
//        when(stockDetailsRepository.save(any(StockDetails.class))).thenReturn(stock);
//        doNothing().when(historyService).saveStockHistory(any(StockDetails.class), anyString());
//        doNothing().when(historyService).saveOrderHistory(any(VehicleOrderDetails.class), anyString(), any(OrderStatus.class));
//
//        when(vehicleOrderDetailsRepository.saveAndFlush(any(VehicleOrderDetails.class)))
//                .thenAnswer(invocation -> {
//                    VehicleOrderDetails saved = invocation.getArgument(0);
//                    saved.setCustomerOrderId("ORDER-123456");
//                    saved.setOrderStatus(OrderStatus.BLOCKED);
//                    return saved;
//                });
//
//        OrderResponse response = new OrderResponse();
//        response.setCustomerOrderId("ORDER-123456");
//        response.setCustomerName("Test Customer");
//        response.setOrderStatus(OrderStatus.BLOCKED);
//        response.setQuantity(1);
//        response.setModelName("Test Model");
//        response.setVariant("Test Variant");
//        response.setColour("Red");
//        response.setFuelType("Petrol");
//        response.setTransmissionType("Manual");
//        response.setVehicleModelId(1L);
//        response.setVehicleVariantId(1L);
//
//        // Mock checkAndBlockStock instead of processOrder
//        when(vehicleOrderService.checkAndBlockStock(any(OrderRequest.class))).thenReturn(response);
//
//        // 4. Perform the mock request and assert
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/placeOrder")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(multiOrderRequest)))
//                .andExpect(status().isAccepted())
//                .andExpect(jsonPath("$.statusCode").value(200)) // Matches HttpStatus.OK.value()
//                .andExpect(jsonPath("$.statusMessage").value(containsString("Multiple orders placed successfully")))
//                .andExpect(jsonPath("$.data.orderResponses[0].customerOrderId").value("ORDER-123456"))
//                .andExpect(jsonPath("$.data.orderResponses[0].orderStatus").value("BLOCKED"));
//    }

    @Test
    void testPlaceOrder_ValidationFailure() throws Exception {
        OrderRequest request = new OrderRequest();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/placeOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(is(400)))
                .andExpect(jsonPath("$.statusMessage").exists());
    }



    @Test
    void testCancelOrder_SuccessFallback() throws Exception {
        String customerOrderId = "123";

        // Mock VehicleModel and VehicleVariant
        VehicleModel mockModel = new VehicleModel();
        mockModel.setVehicleModelId(1L); // Valid ID
        VehicleVariant mockVariant = new VehicleVariant();
        mockVariant.setVehicleVariantId(1L); // Valid ID

        // Configure mockOrderDetails with required fields
        VehicleOrderDetails mockOrderDetails = new VehicleOrderDetails();
        mockOrderDetails.setCustomerOrderId(customerOrderId);
        mockOrderDetails.setOrderStatus(OrderStatus.BLOCKED);
        mockOrderDetails.setVehicleModelId(mockModel);
        mockOrderDetails.setVehicleVariantId(mockVariant);
        mockOrderDetails.setModelName("TestModel");
        mockOrderDetails.setVariant("TestVariant");
        mockOrderDetails.setColour("Blue");
        mockOrderDetails.setFuelType("Petrol");
        mockOrderDetails.setTransmissionType("Manual");
        mockOrderDetails.setQuantity(1); // Non-zero quantity

        // Mock repository and service dependencies
        when(vehicleOrderDetailsRepository.findByCustomerOrderId(customerOrderId))
                .thenReturn(Optional.of(mockOrderDetails));
        when(stockDetailsRepository.findByModelNameAndVehicleVariantIdAndStockStatus(
                eq("TestModel"), eq(mockVariant), eq(StockStatus.AVAILABLE)))
                .thenReturn(Collections.emptyList()); // No matching stock
        StockDetails newStock = new StockDetails();
        newStock.setStockId(1L); // Simulate saved stock
        newStock.setVehicleModelId(mockModel);
        newStock.setVehicleVariantId(mockVariant);
        newStock.setModelName("TestModel");
        newStock.setVariant("TestVariant");
        newStock.setColour("Blue");
        newStock.setFuelType("Petrol");
        newStock.setTransmissionType("Manual");
        newStock.setQuantity(1);
        newStock.setStockStatus(StockStatus.AVAILABLE);
        when(stockDetailsRepository.save(any(StockDetails.class))).thenReturn(newStock);
        doNothing().when(historyService).saveStockHistory(any(StockDetails.class), anyString());
        doNothing().when(historyService).saveOrderHistory(any(VehicleOrderDetails.class), eq("system"), eq(OrderStatus.CANCELED));
        when(vehicleOrderDetailsRepository.save(any(VehicleOrderDetails.class))).thenReturn(mockOrderDetails);

        // Mock WorkflowClient to throw exception for fallback path
        when(workflowClient.newWorkflowStub(eq(VehicleSalesParentWorkflow.class), anyString()))
                .thenThrow(new NullPointerException("parentWorkflow is null")); // Simulate workflow failure

        // Perform the request and validate response
        mockMvc.perform(MockMvcRequestBuilders.post("/api/cancelOrder")
                        .param("customerOrderId", customerOrderId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.statusMessage").value(containsString("Order canceled successfully")))
                .andExpect(jsonPath("$.data.customerOrderId").value(customerOrderId))
                .andExpect(jsonPath("$.data.orderStatus").value("CANCELED"));
    }

    @Test
    void testCancelOrder_NotFound() throws Exception {
        String customerOrderId = "NONEXISTENT";

        // Mock repository to return empty for non-existent order
        when(vehicleOrderDetailsRepository.findByCustomerOrderId(customerOrderId))
                .thenReturn(Optional.empty());

        // Mock WorkflowClient to throw exception for fallback path
        when(workflowClient.newWorkflowStub(eq(VehicleSalesParentWorkflow.class), anyString()))
                .thenThrow(new NullPointerException("parentWorkflow is null")); // Simulate workflow failure

        // Perform the request and validate response
        mockMvc.perform(MockMvcRequestBuilders.post("/api/cancelOrder")
                        .param("customerOrderId", customerOrderId)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.statusMessage").value(containsString("Order not found with customerOrderId: " + customerOrderId)))
                .andExpect(jsonPath("$.data").doesNotExist());

        // Verify repository interaction
        verify(vehicleOrderDetailsRepository, times(1)).findByCustomerOrderId(customerOrderId);
    }

    @Test
    void testGetAllOrderCounts_Success() throws Exception {
        when(vehicleOrderService.getTotalOrders()).thenReturn(100L);
        when(vehicleOrderService.getPendingOrders()).thenReturn(20L);
        when(vehicleOrderService.getFinancePendingOrders()).thenReturn(10L);
        when(vehicleOrderService.getClosedOrders()).thenReturn(50L);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orderStats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(100))
                .andExpect(jsonPath("$.data.pendingOrders").value(20))
                .andExpect(jsonPath("$.data.financePendingOrders").value(10))
                .andExpect(jsonPath("$.data.closedOrders").value(50));
    }


    @Test
    void testGetVehicleOrdersForGrid_Success() throws Exception {
        // Include expectedDeliveryDate in the constructor
        VehicleOrderGridDTO dto = new VehicleOrderGridDTO("123", "Test Customer", "Test Model", 1, "Test Variant", OrderStatus.BLOCKED, "2025-07-10");

        KendoGridResponse<VehicleOrderGridDTO> response = new KendoGridResponse<>();
        response.setData(Collections.singletonList(dto));
        response.setTotal(1);

        when(vehicleOrderService.getAllOrders()).thenReturn(response.getData()); // Adjusted to return List

        mockMvc.perform(MockMvcRequestBuilders.get("/api/vehicleorders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].customerOrderId").value(is("123")))
                .andExpect(jsonPath("$.data[0].customerName").value(is("Test Customer")));
    }
    @Test
    void testGetOrderStatusProgress_Success() throws Exception {
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orderstatus/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(is(1)))
                .andExpect(jsonPath("$.data[0].customerOrderId").value(is("123")))
                .andExpect(jsonPath("$.data[0].orderStatus").value(is("BLOCKED")));
    }

    @Test
    void testGetOrderStatusProgress_NotFound() throws Exception {
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/orderstatus/123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.total").value(is(0)));
    }

    // FinanceController Tests
    private FinanceRequest createSampleFinanceRequest() {
        FinanceRequest request = new FinanceRequest();
        request.setCustomerOrderId("123");
        request.setCustomerName("Test Customer");
        request.setVehicleModelId(1L);
        request.setVehicleVariantId(1L);
        request.setModelName("Test Model");
        request.setVariant("Test Variant");
        request.setColour("Red");
        request.setFuelType("Petrol");
        request.setTransmissionType("Manual");
        request.setQuantity(1);
        request.setPaymentMode("Cash");
        return request;
    }

    private FinanceResponse createSampleFinanceResponse(String orderId, String customerName,
                                                        FinanceStatus financeStatus, OrderStatus orderStatus) {
        FinanceResponse response = new FinanceResponse();
        response.setCustomerOrderId(orderId);
        response.setCustomerName(customerName);
        response.setFinanceStatus(financeStatus);
        response.setOrderStatus(orderStatus);
        response.setModelName("Test Model");
        response.setVariant("Test Variant");
        return response;
    }

    private VehicleOrderDetails createSampleOrderDetails(String orderId, OrderStatus status) {
        VehicleOrderDetails orderDetails = new VehicleOrderDetails();
        orderDetails.setCustomerOrderId(orderId);
        orderDetails.setOrderStatus(status);
        orderDetails.setModelName("Test Model");
        orderDetails.setVariant("Test Variant");
        return orderDetails;
    }



    @Test
    void testInitiateFinance_OrderNotFound() throws Exception {
        // Given
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));
        when(financeService.createFinanceDetails(any(FinanceRequest.class))).thenReturn(financeResponse);

        // mocking workflow query to simulate running status
        var stub = Mockito.mock(io.temporal.client.WorkflowStub.class);
        when(workflowClient.newUntypedWorkflowStub("parent-123")).thenReturn(stub);
        when(stub.query(eq("getWorkflowStatus"), eq(String.class))).thenReturn("BLOCKED");

        var workflowMock = Mockito.mock(com.vehicle.salesmanagement.workflow.VehicleSalesParentWorkflow.class);
        when(workflowClient.newWorkflowStub(eq(com.vehicle.salesmanagement.workflow.VehicleSalesParentWorkflow.class), eq("parent-123"), any()))
                .thenReturn(workflowMock);

        doNothing().when(workflowMock).initiateFinance(any(FinanceRequest.class));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeInitiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(financeRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.statusCode", is(202)))
                .andExpect(jsonPath("$.statusMessage", is("Finance details created successfully")))
                .andExpect(jsonPath("$.data.customerOrderId", is("123")))
                .andExpect(jsonPath("$.data.customerName", is("Test Customer")))
                .andExpect(jsonPath("$.data.orderStatus", is("BLOCKED")))
                .andExpect(jsonPath("$.data.modelName", is("Test Model")))
                .andExpect(jsonPath("$.data.variant", is("Test Variant")));
    }
    @Test
    void testInitiateFinance_FinanceAlreadyExists() throws Exception {
        // Given
        FinanceRequest request = createSampleFinanceRequest();

        VehicleOrderDetails orderDetails = new VehicleOrderDetails();
        orderDetails.setCustomerOrderId("123");
        orderDetails.setOrderStatus(OrderStatus.BLOCKED);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));

        when(financeService.createFinanceDetails(any(FinanceRequest.class)))
                .thenThrow(new IllegalStateException("Finance details already exist for order ID: 123"));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeInitiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.statusMessage")
                        .value("Failed to create finance details: Finance details already exist for order ID: 123"));

        verify(vehicleOrderDetailsRepository, times(1)).findByCustomerOrderId("123");
        verify(financeService, times(1)).createFinanceDetails(any(FinanceRequest.class));
    }

    @Test
    void testInitiateFinance_OrderNotBlocked() throws Exception {
        // Given
        FinanceRequest request = createSampleFinanceRequest();

        VehicleOrderDetails orderDetails = new VehicleOrderDetails();
        orderDetails.setCustomerOrderId("123");
        orderDetails.setOrderStatus(OrderStatus.PENDING);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeInitiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.statusMessage").value("Finance initiation failed: Order must be in BLOCKED status"));

        verify(vehicleOrderDetailsRepository, times(1)).findByCustomerOrderId("123");
        verify(financeService, never()).createFinanceDetails(any(FinanceRequest.class));
    }

    @Test
    void testApproveFinance_Success() throws Exception {
        ApproveFinanceRequest request = new ApproveFinanceRequest();
        request.setCustomerOrderId("123");
        request.setApprovedBy("Test Approver");

        WorkflowStub workflowStub = mock(WorkflowStub.class);
        when(workflowClient.newUntypedWorkflowStub("finance-123")).thenReturn(workflowStub);
        doNothing().when(workflowStub).signal(eq("approveFinance"), any());

        FinanceResponse mockResponse = new FinanceResponse();
        mockResponse.setCustomerOrderId("123");
        mockResponse.setFinanceId(1001L);
        mockResponse.setFinanceStatus(FinanceStatus.valueOf("APPROVED"));
        mockResponse.setOrderStatus(OrderStatus.valueOf("ALLOTTED"));
        mockResponse.setCustomerName("Tarun Sai");
        mockResponse.setApprovedBy("Test Approver");
// You can fill in sample values if needed
        when(workflowStub.getResult(FinanceResponse.class)).thenReturn(mockResponse);


        // Perform & Validate
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeApprove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.statusMessage").value("Finance approved successfully"))
                .andExpect(jsonPath("$.data.customerOrderId").value("123"))
                .andExpect(jsonPath("$.data.financeStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.orderStatus").value("ALLOTTED"));

        // Verifications
        //  verify(financeService, times(1)).approveFinance("123", "Test Approver");
        verify(workflowStub, times(1)).signal(anyString(), any());
    }

    @Test
    void testApproveFinance_MissingParameters() throws Exception {
        // Missing approvedBy
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeApprove")
                        .contentType("application/json")
                        .content("""
                {
                    "customerOrderId": "123"
                }
            """))
                .andExpect(status().isBadRequest());

        // Missing customerOrderId
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeApprove")
                        .contentType(MediaType.valueOf("application/json"))
                        .content("""
                {
                    "approvedBy": "Test Approver"
                }
            """))
                .andExpect(status().isBadRequest());

        verify(financeService, never()).approveFinance(anyString(), anyString());
    }

    @Test
    void testApproveFinance_FinanceDetailsNotFound() throws Exception {
        // Given
        String customerOrderId = "999";
        String approvedBy = "Test Approver";

        // Use JSON request body instead of form parameters
        String requestBody = """
        {
            "customerOrderId": "%s",
            "approvedBy": "%s"
        }
        """.formatted(customerOrderId, approvedBy);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeApprove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.statusMessage").value(
                        "Failed to approve finance for workflowId='finance-999': Cannot invoke \"io.temporal.client.WorkflowStub.signal(String, Object[])\" because \"workflow\" is null"
                ));
    }



    @Test
    void testApproveFinance_WorkflowNotFound() throws Exception {
        // Given
        String customerOrderId = "123";
        String approvedBy = "Test Approver";

        // Construct the JSON request body
        String requestBody = """
        {
            "customerOrderId": "%s",
            "approvedBy": "%s"
        }
        """.formatted(customerOrderId, approvedBy);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeApprove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.statusMessage").value(
                        "Failed to approve finance for workflowId='finance-123': Cannot invoke \"io.temporal.client.WorkflowStub.signal(String, Object[])\" because \"workflow\" is null"
                ));
    }

    @Test
    void testRejectFinance_Success() throws Exception {
        // Prepare request
        RejectFinanceRequest request = new RejectFinanceRequest();
        request.setCustomerOrderId("123");
        request.setRejectedBy("Test Rejector");

        // Prepare mocked workflow stub
        WorkflowStub workflowStub = mock(WorkflowStub.class);
        when(workflowClient.newUntypedWorkflowStub("finance-123")).thenReturn(workflowStub);
        doNothing().when(workflowStub).signal(eq("rejectFinance"), any());

        // Prepare mocked finance response
        FinanceResponse mockResponse = new FinanceResponse();
        mockResponse.setCustomerOrderId("123");
        mockResponse.setFinanceId(1002L);
        mockResponse.setFinanceStatus(FinanceStatus.REJECTED);
        mockResponse.setOrderStatus(OrderStatus.PENDING);
        mockResponse.setCustomerName("Tarun Sai");
        mockResponse.setRejectedBy("Test Rejector");

        when(workflowStub.getResult(FinanceResponse.class)).thenReturn(mockResponse);

        // Perform & Validate
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeReject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.statusMessage").value("Finance rejected successfully"))
                .andExpect(jsonPath("$.data.customerOrderId").value("123"))
                .andExpect(jsonPath("$.data.financeStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING"));

        // Verifications
        verify(workflowStub, times(1)).signal(eq("rejectFinance"), eq("Test Rejector"));
        verify(workflowStub, times(1)).getResult(FinanceResponse.class);
    }

    @Test
    void testRejectFinance_MissingParameters() throws Exception {
        // Missing rejectedBy
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeReject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "customerOrderId": "123"
                        }
                    """))
                .andExpect(status().isBadRequest());

        // Missing customerOrderId
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeReject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "rejectedBy": "Test Rejector"
                        }
                    """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(financeService);
    }


    @Test
    void testRejectFinance_FinanceDetailsNotFound() throws Exception {
        // Given
        String customerOrderId = "999";
        String rejectedBy = "Test Rejector";

        String requestBody = """
    {
        "customerOrderId": "%s",
        "rejectedBy": "%s"
    }
    """.formatted(customerOrderId, rejectedBy);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeReject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.statusMessage").value(
                        "Failed to reject finance for workflowId='finance-999': Cannot invoke \"io.temporal.client.WorkflowStub.signal(String, Object[])\" because \"workflow\" is null"
                ));
    }


    @Test
    void testRejectFinance_WorkflowFailed() throws Exception {
        // Given
        String customerOrderId = "123";
        String rejectedBy = "Test Rejector";

        // Construct the JSON request body
        String requestBody = """
    {
        "customerOrderId": "%s",
        "rejectedBy": "%s"
    }
    """.formatted(customerOrderId, rejectedBy);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/financeReject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.statusMessage").value(
                        "Failed to reject finance for workflowId='finance-123': Cannot invoke \"io.temporal.client.WorkflowStub.signal(String, Object[])\" because \"workflow\" is null"
                ));
    }


    // DispatchDeliveryController Tests
    @Test
    void testInitiateDispatch_Success() throws Exception {
        // Set up order details
        orderDetails.setOrderStatus(OrderStatus.ALLOTTED);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));
        when(dispatchDeliveryService.initiateDispatch(any(DispatchRequest.class))).thenReturn(dispatchResponse);

        // Mock Temporal workflow client and stub
        WorkflowStub workflowStub = mock(WorkflowStub.class);
        when(workflowClient.newUntypedWorkflowStub("parent-123")).thenReturn(workflowStub);
        doNothing().when(workflowStub).signal(eq("initiateDispatch"), any(DispatchRequest.class));

        // Perform the request and validate response
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/initiateDispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispatchRequest)))
                .andExpect(status().isOk()) // Changed from isAccepted() to isOk()
                .andExpect(jsonPath("$.statusCode").value(202))
                .andExpect(jsonPath("$.statusMessage").value("Dispatch process initiated for order ID: 123"))
                .andExpect(jsonPath("$.data.customerOrderId").value("123"));
    }

    @Test
    void testInitiateDispatch_WorkflowNotFound() throws Exception {
        orderDetails.setOrderStatus(OrderStatus.ALLOTTED);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));
        when(dispatchDeliveryService.initiateDispatch(any(DispatchRequest.class)))
                .thenThrow(new IllegalStateException("Order not found for dispatch"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/initiateDispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispatchRequest)))
                .andExpect(status().isOk()) // Changed from isNotFound() to isOk()
                .andExpect(jsonPath("$.statusCode").value(400)) // Changed from 404 to 400
                .andExpect(jsonPath("$.statusMessage").value("Failed to initiate dispatch: Order not found for dispatch"))
                .andExpect(jsonPath("$.data").isEmpty()); // Verify data is null
    }

    @Test
    void testInitiateDispatch_ServiceFailure() throws Exception {
        orderDetails.setOrderStatus(OrderStatus.BLOCKED);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));
        when(dispatchDeliveryService.initiateDispatch(any(DispatchRequest.class)))
                .thenThrow(new IllegalStateException("Order must be ALLOTTED"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/initiateDispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispatchRequest)))
                .andExpect(status().isOk()) // Expect HTTP 200 instead of 400
                .andExpect(jsonPath("$.statusCode").value(400)) // Check ApiResponse statusCode
                .andExpect(jsonPath("$.statusMessage").value("Failed to initiate dispatch: Order must be ALLOTTED"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
    @Test
    void testConfirmDelivery_InvalidState() throws Exception {
        orderDetails.setOrderStatus(OrderStatus.ALLOTTED);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));
        when(dispatchDeliveryService.confirmDelivery(any(DeliveryRequest.class)))
                .thenThrow(new IllegalStateException("Order must be DISPATCHED"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/confirmDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryRequest)))
                .andExpect(status().isOk()) // Changed from isBadRequest()
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.statusMessage").value("Failed to confirm delivery: Order must be DISPATCHED"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testConfirmDelivery_Success() throws Exception {
        // Set up order details
        orderDetails.setOrderStatus(OrderStatus.DISPATCHED);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));
        when(dispatchDeliveryService.confirmDelivery(any(DeliveryRequest.class))).thenReturn(deliveryResponse);

        // Mock Temporal workflow client and stub
        WorkflowStub workflowStub = mock(WorkflowStub.class);
        when(workflowClient.newUntypedWorkflowStub("parent-123")).thenReturn(workflowStub);
        doNothing().when(workflowStub).signal(eq("confirmDelivery"), any(DeliveryRequest.class));

        // Perform the request and validate response
        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/confirmDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(is(200)))
                .andExpect(jsonPath("$.statusMessage").value(is("Delivery confirmed successfully")))
                .andExpect(jsonPath("$.data.customerOrderId").value(is("123")));
    }
    @Test
    void testConfirmDelivery_WorkflowNotFound() throws Exception {
        orderDetails.setOrderStatus(OrderStatus.DISPATCHED);
        when(vehicleOrderDetailsRepository.findByCustomerOrderId("123")).thenReturn(Optional.of(orderDetails));
        when(dispatchDeliveryService.confirmDelivery(any(DeliveryRequest.class)))
                .thenThrow(new IllegalStateException("Order not found for delivery"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/order/confirmDelivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveryRequest)))
                .andExpect(status().isOk()) // Expect HTTP 200 instead of 404
                .andExpect(jsonPath("$.statusCode").value(is(400))) // Expect 400 instead of 404
                .andExpect(jsonPath("$.statusMessage").value(is("Failed to confirm delivery: Order not found for delivery")))
                .andExpect(jsonPath("$.data").isEmpty()); // Verify data is null
    }
}