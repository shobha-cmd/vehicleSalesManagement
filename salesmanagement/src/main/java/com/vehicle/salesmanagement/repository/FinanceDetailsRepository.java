package com.vehicle.salesmanagement.repository;


import com.vehicle.salesmanagement.domain.entity.model.FinanceDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceDetailsRepository extends JpaRepository<FinanceDetails, Long> {
    FinanceDetails findByCustomerOrderId(Long customerOrderId);
}