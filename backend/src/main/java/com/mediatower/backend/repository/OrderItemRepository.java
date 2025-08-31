package com.mediatower.backend.repository;

import com.mediatower.backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Pas de méthodes personnalisées spécifiques nécessaires pour l'instant, JpaRepository suffit.
}