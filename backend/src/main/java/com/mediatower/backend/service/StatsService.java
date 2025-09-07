// src/main/java/com/mediatower/backend/service/StatsService.java
package com.mediatower.backend.service;

import com.mediatower.backend.dto.DashboardSummaryDto;
import com.mediatower.backend.dto.ServiceDto;
import com.mediatower.backend.dto.UserStatsDto;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StatsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository; // <-- Dépendance restaurée
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository; // <-- Dépendance restaurée
    private final OrderItemRepository orderItemRepository;
    private final CategoryRepository categoryRepository;
    private final ServiceService serviceService; // <-- Dépendance restaurée

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    // --- CORRECTION : Constructeur complet avec toutes les dépendances ---
    public StatsService(UserRepository userRepository, ProductRepository productRepository, ServiceRepository serviceRepository,
                        OrderRepository orderRepository, BookingRepository bookingRepository,
                        OrderItemRepository orderItemRepository, CategoryRepository categoryRepository,
                        ServiceService serviceService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
        this.orderItemRepository = orderItemRepository;
        this.categoryRepository = categoryRepository;
        this.serviceService = serviceService;
    }

    public DashboardSummaryDto getDashboardSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endCurrentPeriod = now;
        LocalDateTime startCurrentPeriod = endCurrentPeriod.minusDays(30);
        LocalDateTime endPreviousPeriod = startCurrentPeriod;
        LocalDateTime startPreviousPeriod = endPreviousPeriod.minusDays(30);

        // --- KPIs Intelligents ---
        DashboardSummaryDto.KpiDto kpis = new DashboardSummaryDto.KpiDto();

        long newOrdersCurrent = orderRepository.countByOrderDateBetween(startCurrentPeriod, endCurrentPeriod);
        long newOrdersPrevious = orderRepository.countByOrderDateBetween(startPreviousPeriod, endPreviousPeriod);
        kpis.setNewOrders(newOrdersCurrent);
        kpis.setNewOrdersChangePercentage(calculatePercentageChange(newOrdersPrevious, newOrdersCurrent));

        long newUsersCurrent = userRepository.countByCreatedAtBetween(startCurrentPeriod, endCurrentPeriod);
        long newUsersPrevious = userRepository.countByCreatedAtBetween(startPreviousPeriod, endPreviousPeriod);
        kpis.setNewUsers(newUsersCurrent);
        kpis.setNewUsersChangePercentage(calculatePercentageChange(newUsersPrevious, newUsersCurrent));

        kpis.setOutOfStockProducts(productRepository.countByStockLessThanEqual(0));
        kpis.setPendingOrders(orderRepository.countByStatus(OrderStatus.PENDING));

        BigDecimal revenueCurrent = orderRepository.findTotalRevenueBetween(startCurrentPeriod, endCurrentPeriod);
        revenueCurrent = (revenueCurrent == null) ? BigDecimal.ZERO : revenueCurrent;
        BigDecimal revenuePrevious = orderRepository.findTotalRevenueBetween(startPreviousPeriod, endPreviousPeriod);
        revenuePrevious = (revenuePrevious == null) ? BigDecimal.ZERO : revenuePrevious;

        BigDecimal avgOrderValueCurrent = (newOrdersCurrent == 0) ? BigDecimal.ZERO : revenueCurrent.divide(BigDecimal.valueOf(newOrdersCurrent), 2, RoundingMode.HALF_UP);
        BigDecimal avgOrderValuePrevious = (newOrdersPrevious == 0) ? BigDecimal.ZERO : revenuePrevious.divide(BigDecimal.valueOf(newOrdersPrevious), 2, RoundingMode.HALF_UP);
        kpis.setAverageOrderValue(avgOrderValueCurrent);
        kpis.setAverageOrderValueChangePercentage(calculatePercentageChange(avgOrderValuePrevious.doubleValue(), avgOrderValueCurrent.doubleValue()));

        summary.setKpis(kpis);

        // --- Revenus ---
        DashboardSummaryDto.RevenueDto revenue = new DashboardSummaryDto.RevenueDto();
        revenue.setRevenueLast30Days(revenueCurrent);
        revenue.setRevenueChangePercentage(calculatePercentageChange(revenuePrevious.doubleValue(), revenueCurrent.doubleValue()));
        summary.setRevenue(revenue);

        // --- Logique complète pour les graphiques ---
        List<Order> last30DaysOrders = orderRepository.findByOrderDateBetween(startCurrentPeriod, endCurrentPeriod);
        Map<LocalDate, Double> salesByDay = last30DaysOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().toLocalDate(),
                        Collectors.summingDouble(order -> order.getTotalAmount().doubleValue())
                ));
        List<DashboardSummaryDto.ChartDataPoint> chartData = salesByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DashboardSummaryDto.ChartDataPoint(entry.getKey().toString(), new BigDecimal(entry.getValue())))
                .collect(Collectors.toList());
        summary.setSalesLast30Days(chartData);

        summary.setTopSellingProducts(getTopSellingProducts(5));
        summary.setRevenueByCategories(getRevenueByCategories());

        return summary;
    }

    private double calculatePercentageChange(double previous, double current) {
        if (previous == 0) {
            return (current > 0) ? 100.0 : 0.0;
        }
        return ((current - previous) / previous) * 100.0;
    }

    private List<DashboardSummaryDto.TopProductDto> getTopSellingProducts(int limit) {
        List<Object[]> results = orderItemRepository.findTopSellingProducts(PageRequest.of(0, limit));
        return results.stream().map(record -> {
            Long productId = (Long) record[0];
            long totalSold = (Long) record[1];
            return productRepository.findById(productId).map(product -> {
                String imageUrl = product.getMediaAssets().stream()
                        .filter(media -> media.getType() == MediaType.IMAGE && media.isPrimary())
                        .findFirst()
                        .map(media -> backendBaseUrl + "/uploads/products/" + media.getFileName())
                        .orElse(null);
                return new DashboardSummaryDto.TopProductDto(productId, product.getNames().getOrDefault("en", "N/A"), imageUrl, totalSold);
            }).orElse(null);
        }).filter(dto -> dto != null).collect(Collectors.toList());
    }

    private List<DashboardSummaryDto.CategoryRevenueDto> getRevenueByCategories() {
        List<Object[]> results = orderItemRepository.findRevenueByCategory();
        return results.stream().map(record -> {
            Long categoryId = (Long) record[0];
            BigDecimal totalRevenue = BigDecimal.valueOf((Double) record[1]);
            String categoryName = categoryRepository.findById(categoryId)
                    .map(category -> category.getNames().getOrDefault("en", "Catégorie " + categoryId))
                    .orElse("Inconnue");
            return new DashboardSummaryDto.CategoryRevenueDto(categoryId, categoryName, totalRevenue);
        }).collect(Collectors.toList());
    }

    // ==================== MÉTHODE RESTAURÉE ====================
    public UserStatsDto getUserStats(String userUid) {
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));

        UserStatsDto stats = new UserStatsDto();
        List<Order> userOrders = orderRepository.findByUser(user);

        // 1. Calcul des dépenses par mois
        Map<String, Double> spendingByMonthMap = userOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.summingDouble(order -> order.getTotalAmount().doubleValue())
                ));
        List<UserStatsDto.MonthlySpending> monthlySpendings = spendingByMonthMap.entrySet().stream()
                .map(entry -> {
                    UserStatsDto.MonthlySpending ms = new UserStatsDto.MonthlySpending();
                    ms.setMonth(entry.getKey());
                    ms.setTotal(new BigDecimal(entry.getValue()));
                    return ms;
                })
                .sorted(Comparator.comparing(UserStatsDto.MonthlySpending::getMonth))
                .collect(Collectors.toList());
        stats.setSpendingByMonth(monthlySpendings);

        // 2. Calcul des dépenses par catégorie
        Map<String, Double> spendingByCategoryMap = userOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
                .flatMap(order -> order.getOrderItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory().getNames().getOrDefault("en", "Unknown"),
                        Collectors.summingDouble(item -> item.getSubtotal().doubleValue())
                ));
        List<UserStatsDto.SpendingByCategory> spendingByCategories = spendingByCategoryMap.entrySet().stream()
                .map(entry -> {
                    UserStatsDto.SpendingByCategory sbc = new UserStatsDto.SpendingByCategory();
                    sbc.setCategoryName(entry.getKey());
                    sbc.setTotal(new BigDecimal(entry.getValue()));
                    return sbc;
                })
                .collect(Collectors.toList());
        stats.setSpendingByCategory(spendingByCategories);

        return stats;
    }
    // ==============================================================
}