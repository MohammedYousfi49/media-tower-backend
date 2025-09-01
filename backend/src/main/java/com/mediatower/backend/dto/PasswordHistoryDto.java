package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class PasswordHistoryDto {
    private LocalDateTime changeDate;
    private String changeMethod;
    private String ipAddress;
}