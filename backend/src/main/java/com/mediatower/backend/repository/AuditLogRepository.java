package com.mediatower.backend.repository;

import com.mediatower.backend.model.AuditLog;
import com.mediatower.backend.model.SecurityActionType;
import com.mediatower.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Méthode pour l'admin, avec pagination
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    List<AuditLog> findTop10ByUserAndActionOrderByTimestampDesc(User user, SecurityActionType action);
}
