package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {
    private Long id;
    private String customerName;
    private String customerEmail;
    private String serviceName;
    private String status;
    private String assignedAdminName;
    private String customerNotes;
    private LocalDateTime createdAt;
    private BigDecimal servicePrice;
    private String serviceImageUrl;
}