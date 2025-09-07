package com.mediatower.backend.service;

import com.mediatower.backend.dto.AuditLogDto;
import com.mediatower.backend.model.AuditLog;
import com.mediatower.backend.model.SecurityActionType;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        Page<AuditLog> logsPage = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        return logsPage.map(this::convertToDto); // On utilise "map" pour convertir la page d'entités en page de DTOs
    }
    private AuditLogDto convertToDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        dto.setAction(log.getAction());
        dto.setIpAddress(log.getIpAddress());
        dto.setDetails(log.getDetails());
        dto.setTimestamp(log.getTimestamp());

        if (log.getUser() != null) {
            AuditLogDto.UserSummaryDto userDto = new AuditLogDto.UserSummaryDto();
            userDto.setId(log.getUser().getId());
            userDto.setEmail(log.getUser().getEmail());
            dto.setUser(userDto);
        }

        return dto;
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