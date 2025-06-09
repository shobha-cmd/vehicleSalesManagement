package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.DeliveryDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryDetailsRepository extends JpaRepository<DeliveryDetails, Long> {
    DeliveryDetails findByCustomerOrderId(String customerOrderId);
}