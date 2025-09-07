// src/main/java/com/mediatower/backend/repository/UserRepository.java
package com.mediatower.backend.repository;

import com.mediatower.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUid(String uid);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.uid = :uid")
    Optional<User> findUserByUid(@Param("uid") String uid);
    List<User> findAllByUidIn(Collection<String> uids);
    Optional<User> findByVerificationToken(String token);
    long countByCreatedAtAfter(LocalDateTime date);

    // --- AJOUT POUR LES KPIS INTELLIGENTS ---
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    // ---------------------------------------
    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable
    );
}