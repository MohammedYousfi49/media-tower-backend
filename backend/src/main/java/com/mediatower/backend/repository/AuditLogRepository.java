package com.mediatower.backend.repository;

import com.mediatower.backend.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Méthode pour l'admin, avec pagination
    Page<AuditLog> findByOrderByTimestampDesc(Pageable pageable);
}