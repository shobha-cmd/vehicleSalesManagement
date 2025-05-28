//package com.vehicle.salesmanagement.config;
//
//import com.vehicle.salesmanagement.activity.DispatchDeliveryActivitiesImpl;
//import com.vehicle.salesmanagement.workflow.DispatchDeliveryWorkflowImpl;
//import io.temporal.client.WorkflowClient;
//import io.temporal.serviceclient.WorkflowServiceStubs;
//import io.temporal.worker.Worker;
//import io.temporal.worker.WorkerFactory;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class DispatchDeliveryTemporalWorkerConfig {
//
//    private final DispatchDeliveryActivitiesImpl dispatchDeliveryActivities;
//
//    @Bean
//    public WorkflowClient dispatchDeliveryWorkflowClient(WorkflowServiceStubs workflowServiceStubs) {
//        log.info("Creating WorkflowClient for Dispatch and Delivery");
//        return WorkflowClient.newInstance(workflowServiceStubs);
//    }
//
//    @Bean
//    public WorkerFactory dispatchDeliveryWorkerFactory(WorkflowClient dispatchDeliveryWorkflowClient) {
//        log.info("Creating WorkerFactory for Dispatch and Delivery");
//        return WorkerFactory.newInstance(dispatchDeliveryWorkflowClient);
//    }
//
//    @Bean
//    public Worker dispatchDeliveryWorker(WorkerFactory dispatchDeliveryWorkerFactory) {
//        log.info("Configuring Temporal worker for task queue: dispatch-delivery-task-queue");
//        try {
//            Worker worker = dispatchDeliveryWorkerFactory.newWorker("dispatch-delivery-task-queue");
//            worker.registerWorkflowImplementationTypes(DispatchDeliveryWorkflowImpl.class);
//            worker.registerActivitiesImplementations(dispatchDeliveryActivities);
//            dispatchDeliveryWorkerFactory.start();
//            log.info("Temporal worker configured and started successfully for task queue: dispatch-delivery-task-queue");
//            return worker;
//        } catch (Exception e) {
//            log.error("Failed to configure or start Temporal worker for Dispatch and Delivery: {}", e.getMessage(), e);
//            throw new RuntimeException("Dispatch and Delivery worker configuration failed", e);
//        }
//    }
//}