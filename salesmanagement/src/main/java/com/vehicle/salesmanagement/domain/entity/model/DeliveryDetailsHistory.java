package com.vehicle.salesmanagement.domain.entity.model;

import com.vehicle.salesmanagement.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "delivery_details_history",schema="sales_tracking")
public class DeliveryDetailsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryDetails deliveryDetails;

    @Column(name = "customer_order_id", nullable = false, length = 20)
    private String customerOrderId;

    @Column(name = "customer_name")
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus deliveryStatus;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @Column(name = "delivered_by")
    private String deliveredBy;

    @Column(name = "recipient_name")
    private String recipientName;

//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @Column(name = "created_by", length = 100)
//    private String createdBy;
//
//    @Column(name = "updated_by", length = 100)
//    private String updatedBy;

    @Column(name = "delivery_status_history")
    private String deliveryStatusHistory;
}