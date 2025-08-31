package com.mediatower.backend.repository;

import com.mediatower.backend.model.Order;
import com.mediatower.backend.model.User;
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
    // Trouver toutes les commandes d'un utilisateur spécifique
    List<Order> findByUser(User user);
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    // --- NOUVELLE MÉTHODE POUR LA VÉRIFICATION D'AVIS ---
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi WHERE o.user = :user AND oi.product.id = :productId")
    boolean hasUserPurchasedProduct(@Param("user") User user, @Param("productId") Long productId);
    // --- NOUVELLES MÉTHODES POUR LES STATS ---
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal findTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate >= :startDate")
    BigDecimal findRevenueSince(@Param("startDate") LocalDateTime startDate);

    List<Order> findTop5ByOrderByIdDesc();

    @Query("SELECT o FROM Order o JOIN o.payment p WHERE p.transactionId = :transactionId")
    Optional<Order> findByPaymentTransactionId(@Param("transactionId") String transactionId);

    long countByOrderItemsProductId(Long productId);




}