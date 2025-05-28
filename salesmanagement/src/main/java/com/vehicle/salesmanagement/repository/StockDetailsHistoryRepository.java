package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.StockDetailsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDetailsHistoryRepository extends JpaRepository<StockDetailsHistory, Long> {
}