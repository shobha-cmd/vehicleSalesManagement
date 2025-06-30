package com.vehicle.salesmanagement.config;

import com.vehicle.salesmanagement.activity.DispatchDeliveryActivitiesImpl;
import com.vehicle.salesmanagement.activity.FinanceActivitiesImpl;
import com.vehicle.salesmanagement.activity.VehicleOrderActivitiesImpl;
import com.vehicle.salesmanagement.workflow.FinanceWorkflowImpl;
import com.vehicle.salesmanagement.workflow.VehicleOrderWorkflowImpl;
import com.vehicle.salesmanagement.workflow.VehicleSalesParentWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Profile("!test")
public class TemporalWorkerConfig {

    private final VehicleOrderActivitiesImpl vehicleOrderActivities;
    private final FinanceActivitiesImpl financeActivities;
    private final DispatchDeliveryActivitiesImpl dispatchDeliveryActivities;

    private Worker vehicleOrderWorker;
    private Worker financeWorker;
    private Worker dispatchDeliveryWorker;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("Creating WorkflowServiceStubs for Temporal connection");
        try {
            return WorkflowServiceStubs.newServiceStubs(
                    WorkflowServiceStubsOptions.newBuilder()
                            .setTarget("localhost:7233")
                            .setRpcTimeout(Duration.ofSeconds(120)) // Increased timeout
                            .setRpcLongPollTimeout(Duration.ofSeconds(90)) // Explicit long poll timeout
                            .setConnectionBackoffResetFrequency(Duration.ofSeconds(10)) // Retry connection
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create WorkflowServiceStubs: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        log.info("Creating WorkflowClient for Vehicle Order");
        return WorkflowClient.newInstance(
                workflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace("default") // Kept as default per previous working setup
                        .build()
        );
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        log.info("Creating WorkerFactory for Vehicle Order");
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public Worker vehicleOrderWorker(WorkerFactory workerFactory) {
        log.info("Configuring Temporal worker for task queue: vehicle-order-task-queue");
        try {
            Worker worker = workerFactory.newWorker("vehicle-order-task-queue");
            worker.registerWorkflowImplementationTypes(
                    VehicleSalesParentWorkflowImpl.class,
                    VehicleOrderWorkflowImpl.class
            );
            worker.registerActivitiesImplementations(vehicleOrderActivities, dispatchDeliveryActivities);
            this.vehicleOrderWorker = worker;
            return worker;
        } catch (Exception e) {
            log.error("Failed to configure or start Temporal worker: {}", e.getMessage(), e);
            throw new RuntimeException("Worker configuration failed", e);
        }
    }

    @Bean
    public Worker financeWorker(WorkerFactory workerFactory) {
        log.info("Configuring Temporal worker for task queue: finance-task-queue");
        try {
            Worker worker = workerFactory.newWorker("finance-task-queue");
            worker.registerWorkflowImplementationTypes(FinanceWorkflowImpl.class);
            worker.registerActivitiesImplementations(financeActivities);
            this.financeWorker = worker;
            return worker;
        } catch (Exception e) {
            log.error("Failed to configure or start Temporal worker: {}", e.getMessage(), e);
            throw new RuntimeException("Worker configuration failed", e);
        }
    }

    @Bean
    public Worker dispatchDeliveryWorker(WorkerFactory workerFactory) {
        log.info("Configuring Temporal worker for task queue: dispatch-delivery-task-queue");
        try {
            Worker worker = workerFactory.newWorker("dispatch-delivery-task-queue");
            worker.registerActivitiesImplementations(dispatchDeliveryActivities);
            this.dispatchDeliveryWorker = worker;
            return worker;
        } catch (Exception e) {
            log.error("Failed to configure or start Temporal worker: {}", e.getMessage(), e);
            throw new RuntimeException("Worker configuration failed", e);
        }
    }

    @Bean
    public CommandLineRunner startWorkerFactory(WorkerFactory workerFactory) {
        return args -> {
            log.info("Starting WorkerFactory");
            workerFactory.start();
        };
    }

    @Scheduled(fixedRate = 60_000)
    public void logWorkerStatus() {
        if (vehicleOrderWorker != null) {
            log.info("VehicleOrderWorker for task queue 'vehicle-order-task-queue' is active");
        } else {
            log.error("VehicleOrderWorker for task queue 'vehicle-order-task-queue' is not initialized");
        }

        if (financeWorker != null) {
            log.info("FinanceWorker for task queue 'finance-task-queue' is active");
        } else {
            log.error("FinanceWorker for task queue 'finance-task-queue' is not initialized");
        }

        if (dispatchDeliveryWorker != null) {
            log.info("DispatchDeliveryWorker for task queue 'dispatch-delivery-task-queue' is active");
        } else {
            log.error("DispatchDeliveryWorker for task queue 'dispatch-delivery-task-queue' is not initialized");
        }
    }
}