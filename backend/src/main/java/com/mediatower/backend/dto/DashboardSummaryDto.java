package com.mediatower.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardSummaryDto {

    private KpiDto kpis;
    private RevenueDto revenue;
    private List<ChartDataPoint> salesLast30Days;
    private List<BookingDto> recentBookings;
    private List<OrderDto> recentOrders;
    private List<ServiceDto> popularServices; // Ajout du nouveau champ

    @Data
    public static class KpiDto {
        private long totalUsers;
        private long totalProducts;
        private long totalServices;
        private long totalOrders;
        private long totalBookings;
    }

    @Data
    public static class RevenueDto {
        private BigDecimal totalRevenue;
        private BigDecimal revenueLast30Days;
    }

    @Data
    public static class ChartDataPoint {
        private String date;
        private BigDecimal amount;
    }
}