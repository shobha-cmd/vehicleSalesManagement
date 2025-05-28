package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetailsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleOrderDetailsHistoryRepository extends JpaRepository<VehicleOrderDetailsHistory, Long> {
}