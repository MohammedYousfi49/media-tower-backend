package com.mediatower.backend.repository;

import com.mediatower.backend.model.PasswordHistory;
import com.mediatower.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    // Trouve les 5 derniers changements de mot de passe pour un utilisateur, du plus récent au plus ancien
    List<PasswordHistory> findTop5ByUserOrderByChangeDateDesc(User user);
}