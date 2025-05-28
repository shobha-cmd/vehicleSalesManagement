package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.VehicleOrderDetails;
import com.vehicle.salesmanagement.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleOrderDetailsRepository extends JpaRepository<VehicleOrderDetails, Long> {
    long countByOrderStatus(OrderStatus orderStatus);

    Optional<VehicleOrderDetails> findByCustomerOrderId(Long customerOrderId);
}