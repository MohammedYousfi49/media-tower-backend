package com.mediatower.backend.service;

import com.mediatower.backend.dto.OrderDto;
import com.mediatower.backend.dto.OrderItemDto;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final DeliveryService deliveryService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        ProductRepository productRepository,
                        @Lazy DeliveryService deliveryService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.deliveryService = deliveryService;
    }

    @Transactional
    public OrderDto updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));

        if (order.getStatus() == status) {
            return convertToDto(order);
        }

        // ====================== CORRECTION CLÉ ======================
        // On met à jour le statut initial
        order.setStatus(status);
        order = orderRepository.save(order); // Sauvegarder immédiatement
        logger.info("Order {} status updated to {}.", id, status);

        // Si le statut est CONFIRMED, on déclenche le processus de livraison
        if (status == OrderStatus.CONFIRMED) {
            logger.info("Order {} confirmed. Triggering delivery process.", id);

            // Le DeliveryService va gérer l'accès, l'email et changer le statut à DELIVERED
            // IMPORTANT: On passe l'ordre déjà sauvegardé pour éviter les problèmes de transaction
            Order deliveredOrder = deliveryService.processOrderDelivery(order);

            // On retourne directement l'ordre retourné par le DeliveryService
            return convertToDto(deliveredOrder);
        }
        // =============================================================

        return convertToDto(order);
    }

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        return convertToDto(order);
    }

    public List<OrderDto> getOrdersByUserId(String userUid) {
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));
        return orderRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto createOrder(String userUid, OrderDto orderDto) {
        if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
            throw new RuntimeException("Cannot create an order with no items.");
        }
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = orderDto.getOrderItems().stream().map(itemDto -> {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + itemDto.getProductId()));
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(itemDto.getQuantity())));
            return orderItem;
        }).collect(Collectors.toList());

        order.setOrderItems(orderItems);
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Order total amount must be greater than zero.");
        }
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        return convertToDto(savedOrder);
    }

    public boolean canUserReviewProduct(String userUid, Long productId) {
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));
        return orderRepository.hasUserPurchasedProduct(user, productId);
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with ID: " + id);
        }
        orderRepository.deleteById(id);
    }

    public OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());

        if (order.getUser() != null) {
            OrderDto.UserInfo userInfo = new OrderDto.UserInfo(
                    order.getUser().getUid(),
                    order.getUser().getFirstName(),
                    order.getUser().getLastName(),
                    order.getUser().getEmail()
            );
            dto.setUser(userInfo);
        }

        dto.setOrderDate(order.getOrderDate());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());

        if (order.getOrderItems() != null) {
            dto.setOrderItems(order.getOrderItems().stream()
                    .map(item -> {
                        OrderItemDto itemDto = new OrderItemDto();
                        itemDto.setId(item.getId());
                        if (item.getProduct() != null) {
                            itemDto.setProductId(item.getProduct().getId());
                            String productName = item.getProduct().getNames() != null
                                    ? item.getProduct().getNames().getOrDefault("en", "N/A")
                                    : "N/A";
                            itemDto.setProductName(productName);
                        }
                        itemDto.setQuantity(item.getQuantity());
                        itemDto.setUnitPrice(item.getUnitPrice());
                        itemDto.setSubtotal(item.getSubtotal());
                        return itemDto;
                    })
                    .collect(Collectors.toList()));
        }

        if (order.getPayment() != null) {
            dto.setPaymentMethod(order.getPayment().getPaymentMethod());
        }

        return dto;
    }
}