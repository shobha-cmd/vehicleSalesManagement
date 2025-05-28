package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.MddpStock;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import com.vehicle.salesmanagement.enums.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MddpStockRepository extends JpaRepository<MddpStock, Long> { // Adjust ID type if needed
    Optional<MddpStock> findByVehicleVariantIdAndStockStatus(VehicleVariant vehicleVariantId, StockStatus stockStatus);
}