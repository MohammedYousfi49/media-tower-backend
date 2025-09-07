package com.mediatower.backend.controller;

import com.mediatower.backend.dto.AuditLogDto;
import com.mediatower.backend.service.AuditLogService;
import com.mediatower.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuditLogService auditLogService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    // Injection par constructeur
    public AdminController(AuditLogService auditLogService, UserService userService) {
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    // ▼▼▼ MÉTHODE MODIFIÉE ▼▼▼
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(Pageable pageable) {
        Page<AuditLogDto> logs = auditLogService.getAuditLogs(pageable);
        return ResponseEntity.ok(logs);
    }
}