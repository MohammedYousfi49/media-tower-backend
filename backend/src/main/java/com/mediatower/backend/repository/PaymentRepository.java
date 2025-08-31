package com.mediatower.backend.repository;

import com.mediatower.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Fichier: src/main/java/com/mediatower/backend/repository/PaymentRepository.java
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
}