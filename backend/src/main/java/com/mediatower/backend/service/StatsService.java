package com.mediatower.backend.service;

import com.mediatower.backend.dto.*;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private final ServiceRepository serviceRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;

    private final OrderService orderService;
    private final BookingService bookingService;
    private final ServiceService serviceService;

    public StatsService(UserRepository userRepository, ProductRepository productRepository, ServiceRepository serviceRepository, OrderRepository orderRepository, BookingRepository bookingRepository, OrderService orderService, BookingService bookingService, ServiceService serviceService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
        this.orderService = orderService;
        this.bookingService = bookingService;
        this.serviceService = serviceService;
    }

    public DashboardSummaryDto getDashboardSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();

        // Remplir les KPIs
        DashboardSummaryDto.KpiDto kpis = new DashboardSummaryDto.KpiDto();
        kpis.setTotalUsers(userRepository.count());
        kpis.setTotalProducts(productRepository.count());
        kpis.setTotalServices(serviceRepository.count());
        kpis.setTotalOrders(orderRepository.count());
        kpis.setTotalBookings(bookingRepository.count());
        summary.setKpis(kpis);

        // Remplir les revenus
        DashboardSummaryDto.RevenueDto revenue = new DashboardSummaryDto.RevenueDto();
        revenue.setTotalRevenue(orderRepository.findTotalRevenue());
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        revenue.setRevenueLast30Days(orderRepository.findRevenueSince(thirtyDaysAgo));
        summary.setRevenue(revenue);

        // Remplir les données du graphique des ventes
        List<Order> last30DaysOrders = orderRepository.findByOrderDateBetween(thirtyDaysAgo, LocalDateTime.now());
        Map<LocalDate, Double> salesByDay = last30DaysOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().toLocalDate(),
                        Collectors.summingDouble(order -> order.getTotalAmount().doubleValue())
                ));
        List<DashboardSummaryDto.ChartDataPoint> chartData = salesByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    DashboardSummaryDto.ChartDataPoint point = new DashboardSummaryDto.ChartDataPoint();
                    point.setDate(entry.getKey().toString());
                    point.setAmount(new java.math.BigDecimal(entry.getValue()));
                    return point;
                })
                .collect(Collectors.toList());
        summary.setSalesLast30Days(chartData);

        // Remplir les activités récentes et les services populaires
        summary.setRecentOrders(
                orderRepository.findTop5ByOrderByIdDesc().stream()
                        .map(orderService::convertToDto)
                        .collect(Collectors.toList())
        );
        summary.setRecentBookings(
                bookingRepository.findTop5ByOrderByIdDesc().stream()
                        .map(bookingService::convertToDto)
                        .collect(Collectors.toList())
        );
        summary.setPopularServices(this.getPopularServices(5));

        return summary;
    }

    public List<ServiceDto> getPopularServices(int limit) {
        List<Booking> allBookings = bookingRepository.findAll();

        Map<Long, Long> serviceBookingCount = allBookings.stream()
                .collect(Collectors.groupingBy(
                        booking -> booking.getService().getId(),
                        Collectors.counting()
                ));

        List<Long> topServiceIds = serviceBookingCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topServiceIds.isEmpty()) {
            return serviceRepository.findAll().stream()
                    .sorted(Comparator.comparing(Service::getId).reversed())
                    .limit(limit)
                    .map(serviceService::convertToDto)
                    .collect(Collectors.toList());
        }

        List<Service> popularServices = serviceRepository.findAllById(topServiceIds);
        return popularServices.stream()
                .sorted(Comparator.comparing(s -> topServiceIds.indexOf(s.getId())))
                .map(serviceService::convertToDto)
                .collect(Collectors.toList());
    }
    // --- NOUVELLE MÉTHODE POUR LES STATS CLIENT ---
    public UserStatsDto getUserStats(String userUid) {
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));

        UserStatsDto stats = new UserStatsDto();

        // 1. Calcul des dépenses par mois (pour le graphique en barres)
        List<Order> userOrders = orderRepository.findByUser(user);
        Map<String, Double> spendingByMonthMap = userOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED) // On ne compte que les commandes payées
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

        // 2. Calcul des dépenses par catégorie (pour le diagramme circulaire)
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
}