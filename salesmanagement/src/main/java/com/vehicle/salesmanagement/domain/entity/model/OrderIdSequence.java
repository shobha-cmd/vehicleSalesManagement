package com.vehicle.salesmanagement.domain.entity.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "order_id_sequence", schema = "sales_tracking")
public class OrderIdSequence {

    @Id
    @Column(name = "year", length = 4)
    private String year;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;
}