package com.mediatower.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesStatsDto {
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Long totalProductsSold;
}