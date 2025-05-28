package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.DeliveryDetailsHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryDetailsHistoryRepository extends JpaRepository<DeliveryDetailsHistory, Long> {

}