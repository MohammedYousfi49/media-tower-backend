// Chemin : src/main/java/com/mediatower/backend/service/OrderConfirmationService.java
package com.mediatower.backend.service;

import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.OrderRepository;
import com.mediatower.backend.repository.ProductPackRepository;
import com.mediatower.backend.repository.UserProductAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class OrderConfirmationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderConfirmationService.class);

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final UserProductAccessRepository userProductAccessRepository;
    private final ProductPackRepository packRepository;

    public OrderConfirmationService(OrderRepository orderRepository, NotificationService notificationService, UserProductAccessRepository userProductAccessRepository, ProductPackRepository packRepository) {
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.userProductAccessRepository = userProductAccessRepository;
        this.packRepository = packRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executePostConfirmationLogic(Long orderId) {
        logger.info("Executing post-confirmation logic in a new transaction for orderId: {}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found for post-confirmation logic: " + orderId));

            if (order.getStatus() != OrderStatus.CONFIRMED) {
                logger.warn("executePostConfirmationLogic called for order {} but its status is not CONFIRMED. Current status: {}", orderId, order.getStatus());
                return;
            }

            User customer = order.getUser();
            for (OrderItem item : order.getOrderItems()) {
                Product purchasedProduct = item.getProduct();
                String purchasedProductName = purchasedProduct.getNames() != null
                        ? purchasedProduct.getNames().getOrDefault("en", "Unnamed Product")
                        : "Unnamed Product";

                Optional<ProductPack> packOptional = packRepository.findAll().stream()
                        .filter(pack -> pack.getNames() != null && Objects.equals(purchasedProductName, pack.getNames().get("en")))
                        .findFirst();

                if (packOptional.isPresent()) {
                    ProductPack pack = packOptional.get();
                    if (pack.getProducts() != null) {
                        for (Product childProduct : pack.getProducts()) {
                            if (!userProductAccessRepository.existsByUserIdAndProductId(customer.getId(), childProduct.getId())) {
                                UserProductAccess access = new UserProductAccess(customer, childProduct);
                                userProductAccessRepository.save(access);
                            }
                        }
                    }
                } else {
                    if (!userProductAccessRepository.existsByUserIdAndProductId(customer.getId(), purchasedProduct.getId())) {
                        UserProductAccess access = new UserProductAccess(customer, purchasedProduct);
                        userProductAccessRepository.save(access);
                    }
                }
                notificationService.createAdminNotification(
                        "New sale: '" + purchasedProductName + "' sold to " + customer.getEmail(),
                        "NEW_SALE"
                );
            }
            logger.info("Successfully executed post-confirmation logic for orderId: {}", orderId);
        } catch (Exception e) {
            logger.error("CRITICAL: Post-confirmation logic failed for orderId {}. The order is confirmed but user may not have access to products.", orderId, e);
        }
    }
}