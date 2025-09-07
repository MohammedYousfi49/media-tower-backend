package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class LoginHistoryDto {
    private LocalDateTime timestamp;
    private String ipAddress;
    private String details; // ex: "Méthode : TOTP_SUCCESS"
}