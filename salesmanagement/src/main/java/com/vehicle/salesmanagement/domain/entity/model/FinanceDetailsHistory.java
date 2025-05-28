package com.vehicle.salesmanagement.domain.entity.model;

import com.vehicle.salesmanagement.enums.FinanceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "finance_details_history")
public class FinanceDetailsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finance_id")
    private FinanceDetails financeDetails;

    @Column(nullable = false)
    private Long customerOrderId;

    @Column(nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinanceStatus financeStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String approvedBy;

    private String rejectedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;


    @Column(length = 1000)
    private String financeStatusHistory;

    @PrePersist
    public void prePersist() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}