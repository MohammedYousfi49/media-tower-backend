package com.mediatower.backend.repository;

import com.mediatower.backend.model.Booking;
import com.mediatower.backend.model.BookingStatus;
import com.mediatower.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {


    // Nouvelle méthode pour la tâche automatisée
    List<Booking> findByStatusAndPaymentDueDateBefore(BookingStatus status, LocalDateTime now);
    List<Booking> findTop5ByOrderByIdDesc();
    List<Booking> findByCustomer(User customer);




}
