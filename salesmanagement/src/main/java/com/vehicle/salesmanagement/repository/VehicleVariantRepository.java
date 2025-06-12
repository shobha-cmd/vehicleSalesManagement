package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.VehicleModel;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleVariantRepository extends JpaRepository<VehicleVariant, Long> {
    @Query("SELECT v FROM VehicleVariant v WHERE v.vehicleModelId = :vehicleModel")
    List<VehicleVariant> findByVehicleModelId(@Param("vehicleModel") VehicleModel vehicleModelId);


    Optional<VehicleVariant> findByVinNumber(String vinNumber);


    List<VehicleVariant> findByVehicleModelId_ModelNameAndVariant(String modelName, String variant);

    List<VehicleVariant> findByVehicleModelId_ModelName(String modelName);

    List<VehicleVariant> findByVehicleModelId_VehicleModelId(Long vehicleModelId);

    List<VehicleVariant> findByVehicleModelId_VehicleModelIdIn(List<Long> modelIds);

    Optional<VehicleVariant> findByModelNameAndVehicleVariantId(String modelName, Long vehicleVariantId);
}