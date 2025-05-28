package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel,Long> {
    Optional<Object> findByModelName(String modelName);
}
