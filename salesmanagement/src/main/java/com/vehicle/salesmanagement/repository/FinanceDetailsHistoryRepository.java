package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.FinanceDetailsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinanceDetailsHistoryRepository extends JpaRepository<FinanceDetailsHistory, Long> {
}