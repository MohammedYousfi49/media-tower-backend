package com.mediatower.backend.service;

import com.mediatower.backend.model.Order;
import com.mediatower.backend.model.OrderItem;
import com.mediatower.backend.model.OrderStatus;
import com.mediatower.backend.model.UserProductAccess;
import com.mediatower.backend.repository.OrderRepository;
import com.mediatower.backend.repository.UserProductAccessRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    private final OrderRepository orderRepository;
    private final UserProductAccessRepository userProductAccessRepository;
    private final EmailService emailService;

    public DeliveryService(OrderRepository orderRepository,
                           UserProductAccessRepository userProductAccessRepository,
                           EmailService emailService) {
        this.orderRepository = orderRepository;
        this.userProductAccessRepository = userProductAccessRepository;
        this.emailService = emailService;
    }

    /**
     * ====================== CORRECTION CLÉ ======================
     * Cette méthode prend maintenant un objet Order en paramètre
     * au lieu de rechercher par ID, et retourne l'Order mis à jour.
     * Cela évite les problèmes de synchronisation de transactions.
     */
    @Transactional
    public Order processOrderDelivery(Order order) {
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            logger.warn("Attempted to deliver order {} which is not in CONFIRMED state. Current state: {}",
                    order.getId(), order.getStatus());
            return order;
        }

        logger.info("Processing delivery for confirmed order: {}", order.getId());

        // 1. Accorder l'accès aux produits achetés
        for (OrderItem item : order.getOrderItems()) {
            // On vérifie si un accès existe déjà pour éviter les doublons
            boolean accessExists = userProductAccessRepository.existsByUserAndProduct(order.getUser(), item.getProduct());
            if (!accessExists) {
                UserProductAccess access = new UserProductAccess(order.getUser(), item.getProduct());
                userProductAccessRepository.save(access);
                logger.info("Granted access to product '{}' for user '{}'",
                        item.getProduct().getNames().get("en"), order.getUser().getEmail());
            } else {
                logger.info("User '{}' already has access to product '{}'. Skipping.",
                        order.getUser().getEmail(), item.getProduct().getNames().get("en"));
            }
        }

        // 2. Envoyer l'e-mail de confirmation d'achat et d'accès
        try {
            emailService.sendOrderConfirmationEmail(order);
            logger.info("Order confirmation email sent successfully for order {}", order.getId());
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email for order {}. The delivery process will continue, but this needs investigation.",
                    order.getId(), e);
            // IMPORTANT : On ne bloque pas le processus ici. L'accès est donné, c'est le plus important.
        }

        // 3. Mettre à jour le statut final de la commande et retourner l'ordre mis à jour
        order.setStatus(OrderStatus.DELIVERED);
        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} status updated to DELIVERED.", order.getId());

        return savedOrder; // On retourne l'ordre sauvegardé
    }

    /**
     * Méthode de compatibilité pour les appels existants par ID
     * (gardée pour ne pas casser d'autres parties du code si elles existent)
     */
    @Transactional
    public void processOrderDelivery(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));
        processOrderDelivery(order);
    }
}