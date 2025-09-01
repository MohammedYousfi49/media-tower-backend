package com.mediatower.backend.controller;

import com.mediatower.backend.model.AuditLog;
import com.mediatower.backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(Pageable pageable) {
        // Pageable sera automatiquement injecté par Spring (ex: /audit-logs?page=0&size=20)
        Page<AuditLog> logs = auditLogRepository.findByOrderByTimestampDesc(pageable);
        return ResponseEntity.ok(logs);
    }
}