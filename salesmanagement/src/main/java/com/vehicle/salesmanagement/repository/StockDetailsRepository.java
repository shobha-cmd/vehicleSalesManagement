package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.StockDetails;
import com.vehicle.salesmanagement.domain.entity.model.VehicleModel;
import com.vehicle.salesmanagement.domain.entity.model.VehicleVariant;
import com.vehicle.salesmanagement.enums.StockStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockDetailsRepository extends JpaRepository<StockDetails, Long> {
    @Query("SELECT s FROM StockDetails s WHERE s.vehicleVariantId = :vehicleVariant AND s.stockStatus = :stockStatus")
    List<StockDetails> findByVehicleVariantAndStockStatus(
            @Param("vehicleVariant") VehicleVariant vehicleVariant,
            @Param("stockStatus") StockStatus stockStatus
    );

    @Query("SELECT s FROM StockDetails s WHERE s.vehicleVariantId = :variant AND s.vehicleModelId = :model")
    List<StockDetails> findByVehicleVariantAndVehicleModel(
            @Param("variant") VehicleVariant variant,
            @Param("model") VehicleModel model
    );

    List<StockDetails> findByStockStatus(StockStatus stockStatus);


   // Optional<Object> findByVinNumber(String vinNumber);

    List<StockDetails> findByModelNameAndVehicleVariantIdAndStockStatus(@NotBlank(message = "Model name is required") String modelName, VehicleVariant variant, StockStatus stockStatus);

    @Query("SELECT s FROM StockDetails s WHERE s.modelName = :modelName AND s.vehicleVariantId.vehicleVariantId = :vehicleVariantId")
    Optional<StockDetails> findByModelNameAndVehicleVariantIdVariantId(@Param("modelName") String modelName, @Param("vehicleVariantId") Long vehicleVariantId);

}