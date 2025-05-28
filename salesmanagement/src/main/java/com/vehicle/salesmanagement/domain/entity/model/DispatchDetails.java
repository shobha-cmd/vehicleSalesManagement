package com.vehicle.salesmanagement.domain.entity.model;

import com.vehicle.salesmanagement.enums.DispatchStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dispatch_details")
public class DispatchDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dispatchId;

    @Column(name = "customer_order_id", nullable = false)
    private Long customerOrderId;

    @Column(name = "customer_name")
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status")
    private DispatchStatus dispatchStatus;

    @Column(name = "dispatch_date")
    private LocalDateTime dispatchDate;

    @Column(name = "dispatched_by")
    private String dispatchedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}