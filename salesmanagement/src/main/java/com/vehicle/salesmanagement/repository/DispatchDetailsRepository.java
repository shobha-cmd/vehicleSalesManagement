package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.DispatchDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchDetailsRepository extends JpaRepository<DispatchDetails, Long> {
    DispatchDetails findByCustomerOrderId(String customerOrderId);
}