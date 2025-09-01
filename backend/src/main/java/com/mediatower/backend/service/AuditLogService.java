package com.mediatower.backend.service;

import com.mediatower.backend.model.AuditLog;
import com.mediatower.backend.model.SecurityActionType;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async // Important : pour ne pas ralentir la requête principale
    public void logEvent(User user, SecurityActionType action, HttpServletRequest request, String details) {
        String ipAddress = getClientIpAddress(request);
        AuditLog log = new AuditLog(user, action, ipAddress, details);
        auditLogRepository.save(log);
    }

    // Méthode utilitaire pour extraire l'IP
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "N/A";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}