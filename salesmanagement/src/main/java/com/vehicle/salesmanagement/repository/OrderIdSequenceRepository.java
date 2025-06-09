package com.vehicle.salesmanagement.repository;

import com.vehicle.salesmanagement.domain.entity.model.OrderIdSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface OrderIdSequenceRepository extends JpaRepository<OrderIdSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM OrderIdSequence s WHERE s.year = :year")
    OrderIdSequence findByYearWithLock(String year);

    @Modifying
    @Query("UPDATE OrderIdSequence s SET s.sequenceNumber = s.sequenceNumber + 1 WHERE s.year = :year")
    void incrementSequence(String year);
}