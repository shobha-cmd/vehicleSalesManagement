package com.vehicle.salesmanagement.service;

import com.vehicle.salesmanagement.domain.entity.model.OrderIdSequence;
import com.vehicle.salesmanagement.repository.OrderIdSequenceRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class OrderIdGeneratorService {

    private static final String PREFIX = "TYT";
    private static final int SEQUENCE_LENGTH = 3; // For NNN (e.g., 001)

    @Autowired
    private OrderIdSequenceRepository sequenceRepository;

    @Autowired
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateCustomerOrderId() {
        String currentYear = String.valueOf(Year.now().getValue());
        OrderIdSequence sequence = sequenceRepository.findByYearWithLock(currentYear);

        if (sequence == null) {
            // Initialize sequence for the year if it doesn't exist
            sequence = new OrderIdSequence();
            sequence.setYear(currentYear);
            sequence.setSequenceNumber(0L);
            sequenceRepository.save(sequence);
            // Ensure the entity is managed
            sequence = sequenceRepository.findByYearWithLock(currentYear);
            System.out.println("Initialized sequence for year " + currentYear + " with sequence number 0");
        }

        // Log the sequence number before increment
        System.out.println("Before increment, sequence number for year " + currentYear + ": " + sequence.getSequenceNumber());

        // Increment the sequence number in the database
        sequenceRepository.incrementSequence(currentYear);

        // Clear the persistence context to avoid caching issues
        entityManager.flush();
        entityManager.clear();

        // Re-fetch the sequence to get the updated sequence number
        sequence = sequenceRepository.findByYearWithLock(currentYear);
        if (sequence == null) {
            throw new IllegalStateException("Failed to retrieve sequence after increment for year: " + currentYear);
        }

        // Force refresh to ensure we get the latest database state
        entityManager.refresh(sequence);

        Long nextSequence = sequence.getSequenceNumber();
        System.out.println("After increment, sequence number for year " + currentYear + ": " + nextSequence);

        // Format the sequence number with leading zeros (e.g., 001)
        String formattedSequence = String.format("%0" + SEQUENCE_LENGTH + "d", nextSequence);

        // Return the formatted ID (e.g., TYT-2025-001)
        String customerOrderId = String.format("%s-%s-%s", PREFIX, currentYear, formattedSequence);
        System.out.println("Generated customerOrderId: " + customerOrderId);
        return customerOrderId;
    }
}