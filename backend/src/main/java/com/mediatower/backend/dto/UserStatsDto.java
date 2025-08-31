package com.mediatower.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UserStatsDto {

    private List<MonthlySpending> spendingByMonth; // Pour le graphique en barres
    private List<SpendingByCategory> spendingByCategory; // Pour le diagramme circulaire

    @Data
    public static class MonthlySpending {
        private String month; // Ex: "2025-07"
        private BigDecimal total;
    }

    @Data
    public static class SpendingByCategory {
        private String categoryName;
        private BigDecimal total;
    }
}