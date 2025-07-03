package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.MddpStock;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import com.vehicle.salesmanagement.enums.StockStatus;
import io.grpc.MethodDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MddpStockRepository extends JpaRepository<MddpStock, Long> { // Adjust ID type if needed
    Optional<MddpStock> findByVehicleVariantIdAndStockStatus(VehicleVariant vehicleVariantId, StockStatus stockStatus);

    Optional<MddpStock> findByVinNumber(String vinNumber);

    @Query("SELECT m FROM MddpStock m WHERE m.modelName = :modelName AND m.vehicleVariantId.vehicleVariantId = :vehicleVariantId")
    Optional<MddpStock> findByModelNameAndVehicleVariantIdVariantId(@Param("modelName") String modelName, @Param("vehicleVariantId") Long vehicleVariantId);

    List<MddpStock> findAllByVehicleVariantIdAndStockStatus(VehicleVariant vehicleVariantId, StockStatus stockStatus);
}