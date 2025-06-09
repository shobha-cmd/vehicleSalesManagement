package com.vehicle.salesmanagement.domain.entity.model;

import com.vehicle.salesmanagement.enums.FinanceStatus;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "finance_details",schema="sales_tracking")
public class FinanceDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long financeId;

    @Column(name = "customer_order_id", nullable = false, length = 20)
    private String customerOrderId;

    @Column(nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinanceStatus financeStatus; // PENDING, APPROVED, REJECTED

//    private LocalDateTime createdAt;
//
//    private LocalDateTime updatedAt;

    private String approvedBy;

    private String rejectedBy;
}