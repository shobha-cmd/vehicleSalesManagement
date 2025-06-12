package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.ManufacturerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManufacturerOrderRepository extends JpaRepository<ManufacturerOrder, Long> {
    Optional<ManufacturerOrder> findByVinNumber(String vinNumber);

    @Query("SELECT m FROM ManufacturerOrder m WHERE m.modelName = :modelName AND m.vehicleVariantId.vehicleVariantId = :vehicleVariantId")
    Optional<ManufacturerOrder> findByModelNameAndVehicleVariantIdVariantId(@Param("modelName") String modelName, @Param("vehicleVariantId") Long vehicleVariantId);
}