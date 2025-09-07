// Fichier : src/main/java/com/mediatower/backend/repository/BookingRepository.java (COMPLET ET CORRIGÉ)

package com.mediatower.backend.repository;

import com.mediatower.backend.model.Booking;
import com.mediatower.backend.model.BookingStatus;
import com.mediatower.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatusAndPaymentDueDateBefore(BookingStatus status, LocalDateTime now);
    List<Booking> findTop5ByOrderByIdDesc();
    List<Booking> findByCustomer(User customer);
    long countByServiceId(Long serviceId);

    // ▼▼▼ LA CORRECTION EST DANS CETTE REQUÊTE ▼▼▼
    // La jointure sur la collection de noms du service a été corrigée.
    @Query("SELECT DISTINCT b FROM Booking b JOIN b.customer c JOIN b.service s LEFT JOIN s.names sn WHERE " +
            "(:searchTerm IS NULL OR " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "(KEY(sn) IN ('en', 'fr') AND LOWER(VALUE(sn)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) AND " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:adminId IS NULL OR b.assignedAdmin.id = :adminId)")
    Page<Booking> findWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("status") BookingStatus status,
            @Param("adminId") Long adminId,
            Pageable pageable
    );
    // ▲▲▲ FIN DE LA CORRECTION ▲▲▲
}