// src/main/java/com/mediatower/backend/repository/OrderRepository.java
package com.mediatower.backend.repository;

import com.mediatower.backend.model.Order;
import com.mediatower.backend.model.OrderStatus;
import com.mediatower.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi WHERE o.user = :user AND oi.product.id = :productId")
    boolean hasUserPurchasedProduct(@Param("user") User user, @Param("productId") Long productId);
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal findTotalRevenue();
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate >= :startDate")
    BigDecimal findRevenueSince(@Param("startDate") LocalDateTime startDate);
    List<Order> findTop5ByOrderByIdDesc();
    @Query("SELECT o FROM Order o JOIN o.payment p WHERE p.transactionId = :transactionId")
    Optional<Order> findByPaymentTransactionId(@Param("transactionId") String transactionId);
    long countByOrderItemsProductId(Long productId);
    long countByStatus(OrderStatus status);

    // --- CETTE MÉTHODE MANQUAIT PROBABLEMENT ---
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.orderDate >= :startDate AND o.orderDate < :endDate")
    BigDecimal findTotalRevenueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    // ---------------------------------------------

    long countByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    @Query("SELECT o FROM Order o JOIN o.user u WHERE " +
            "(:searchTerm IS NULL OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "CAST(o.id AS string) LIKE CONCAT('%', :searchTerm, '%'))")
    Page<Order> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
}