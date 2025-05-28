package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.ManufacturerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManufacturerOrderRepository extends JpaRepository<ManufacturerOrder,Long> {
}
