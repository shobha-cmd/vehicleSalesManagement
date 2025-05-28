//package com.vehicle.salesmanagement.config;
//
//import com.vehicle.salesmanagement.activity.FinanceActivitiesImpl;
//import com.vehicle.salesmanagement.workflow.FinanceWorkflowImpl;
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
//public class FinanceTemporalWorkerConfig {
//
//    private final FinanceActivitiesImpl financeActivities;
//
//    @Bean
//    public WorkflowClient financeWorkflowClient(WorkflowServiceStubs workflowServiceStubs) {
//        log.info("Creating WorkflowClient for Finance");
//        return WorkflowClient.newInstance(workflowServiceStubs);
//    }
//
//    @Bean
//    public WorkerFactory financeWorkerFactory(WorkflowClient financeWorkflowClient) {
//        log.info("Creating WorkerFactory for Finance");
//        return WorkerFactory.newInstance(financeWorkflowClient);
//    }
//
//    @Bean
//    public Worker financeWorker(WorkerFactory financeWorkerFactory) {
//        log.info("Configuring Temporal worker for task queue: vehicle-order-task-queue");
//        try {
//            Worker worker = financeWorkerFactory.newWorker("vehicle-order-task-queue");
//            worker.registerWorkflowImplementationTypes(FinanceWorkflowImpl.class);
//            worker.registerActivitiesImplementations(financeActivities);
//            financeWorkerFactory.start();
//            log.info("Temporal worker configured and started successfully for task queue: vehicle-order-task-queue");
//            return worker;
//        } catch (Exception e) {
//            log.error("Failed to configure or start Temporal worker for Finance: {}", e.getMessage(), e);
//            throw new RuntimeException("Finance worker configuration failed", e);
//        }
//    }
//}