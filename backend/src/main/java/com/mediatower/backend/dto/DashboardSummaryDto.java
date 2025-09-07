// src/main/java/com/mediatower/backend/dto/DashboardSummaryDto.java
package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardSummaryDto {

    private KpiDto kpis;
    private RevenueDto revenue;
    private List<ChartDataPoint> salesLast30Days;
    private List<TopProductDto> topSellingProducts;
    private List<CategoryRevenueDto> revenueByCategories;

    @Data
    public static class KpiDto {
        private long newOrders;
        private double newOrdersChangePercentage;
        private long newUsers;
        private double newUsersChangePercentage;
        private long outOfStockProducts;
        private long pendingOrders;
        private BigDecimal averageOrderValue;
        private double averageOrderValueChangePercentage;
    }

    @Data
    public static class RevenueDto {
        private BigDecimal revenueLast30Days;
        private double revenueChangePercentage;
    }

    // ==================== CORRECTION ICI ====================
    @Data
    @NoArgsConstructor      // <-- AJOUT : Pour la compatibilité
    @AllArgsConstructor     // <-- AJOUT : C'est ce qui corrige l'erreur
    public static class ChartDataPoint {
        private String date;
        private BigDecimal amount;
    }
    // =======================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDto {
        private Long productId;
        private String productName;
        private String productImageUrl;
        private long totalSold;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRevenueDto {
        private Long categoryId;
        private String categoryName;
        private BigDecimal totalRevenue;
    }
}