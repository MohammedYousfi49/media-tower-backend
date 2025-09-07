package com.mediatower.backend.dto;

import com.mediatower.backend.model.SecurityActionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuditLogDto {

    private Long id;
    private UserSummaryDto user; // On utilise un DTO imbriqué pour les infos de l'utilisateur
    private SecurityActionType action;
    private String ipAddress;
    private String details;
    private LocalDateTime timestamp;

    // DTO interne pour ne renvoyer que l'essentiel de l'utilisateur
    @Getter
    @Setter
    public static class UserSummaryDto {
        private Long id;
        private String email;
    }
}