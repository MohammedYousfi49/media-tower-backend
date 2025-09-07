// src/main/java/com/mediatower/backend/repository/OrderItemRepository.java
package com.mediatower.backend.repository;

import com.mediatower.backend.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Requête pour trouver les produits les plus vendus (ID et quantité)
    @Query("SELECT oi.product.id, SUM(oi.quantity) as totalQuantity FROM OrderItem oi GROUP BY oi.product.id ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProducts(org.springframework.data.domain.Pageable pageable);

    // Requête pour obtenir les revenus par catégorie
    @Query("SELECT oi.product.category.id, SUM(oi.subtotal) as totalRevenue FROM OrderItem oi GROUP BY oi.product.category.id ORDER BY totalRevenue DESC")
    List<Object[]> findRevenueByCategory();
}