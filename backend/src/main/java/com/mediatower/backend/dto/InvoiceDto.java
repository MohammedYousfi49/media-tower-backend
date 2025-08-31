package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    private Long id;
    private String invoiceNumber;
    private Long orderId;
    private LocalDateTime invoiceDate;
    private BigDecimal totalHT;
    private BigDecimal taxAmount;
    private BigDecimal totalTTC;
    private Boolean includesVAT;
    private String billingAddress;
}