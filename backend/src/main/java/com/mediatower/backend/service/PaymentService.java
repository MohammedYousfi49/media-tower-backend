package com.mediatower.backend.service;

import com.mediatower.backend.model.Booking; // <-- AJOUTEZ CET IMPORT
import com.mediatower.backend.model.Order;
import com.mediatower.backend.model.Payment;
import com.mediatower.backend.model.PaymentStatus;
import com.mediatower.backend.repository.BookingRepository; // <-- AJOUTEZ CET IMPORT
import com.mediatower.backend.repository.OrderRepository;
import com.mediatower.backend.repository.PaymentRepository;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PayPalHttpClient payPalHttpClient;
    private final BookingRepository bookingRepository; // <-- AJOUTEZ LE BOOKING REPOSITORY

    // MISE À JOUR DU CONSTRUCTEUR
    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository, PayPalHttpClient payPalHttpClient, BookingRepository bookingRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.payPalHttpClient = payPalHttpClient;
        this.bookingRepository = bookingRepository;
    }

    // ========================================================================
    // == LES MÉTHODES EXISTANTES POUR LES PRODUITS (ORDERS) SONT INCHANGÉES ==
    // ========================================================================

    @Transactional
    public String createStripePaymentIntent(Long orderId) throws StripeException {
        Order order = findOrderOrThrow(orderId);
        createOrUpdatePaymentForOrder(order, "STRIPE");

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(order.getTotalAmount().multiply(new BigDecimal("100")).longValue())
                .setCurrency("mad")
                .putMetadata("orderId", order.getId().toString()) // Métadonnée pour les produits
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        // Associer l'ID de transaction au paiement
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            p.setTransactionId(paymentIntent.getId());
            paymentRepository.save(p);
        });

        logger.info("Created Stripe PaymentIntent {} for order {}", paymentIntent.getId(), orderId);
        return paymentIntent.getClientSecret();
    }

    @Transactional
    public HttpResponse<com.paypal.orders.Order> createPayPalOrder(Long orderId) throws IOException {
        Order order = findOrderOrThrow(orderId);
        createOrUpdatePaymentForOrder(order, "PAYPAL");

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        AmountWithBreakdown amountBreakdown = new AmountWithBreakdown()
                .currencyCode("USD")
                .value(order.getTotalAmount().toString());

        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .amountWithBreakdown(amountBreakdown)
                .customId("order-" + order.getId().toString()); // Préfixe pour les produits

        orderRequest.purchaseUnits(List.of(purchaseUnitRequest));

        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);
        HttpResponse<com.paypal.orders.Order> response = payPalHttpClient.execute(request);

        logger.info("Created PayPal Order {} for order {}", response.result().id(), orderId);
        return response;
    }


    // ================================================================
    // == NOUVELLES MÉTHODES SPÉCIFIQUES POUR LES SERVICES (BOOKINGS) ==
    // ================================================================

    @Transactional
    public String createStripePaymentIntentForBooking(Long bookingId) throws StripeException {
        Booking booking = findBookingOrThrow(bookingId);
        // Note: Pour les services, on ne crée pas d'entité "Payment" pour l'instant,
        // car le statut est déjà géré par l'entité Booking elle-même.
        // On pourrait l'ajouter plus tard si nécessaire.

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(booking.getService().getPrice().multiply(new BigDecimal("100")).longValue())
                .setCurrency("mad")
                .putMetadata("bookingId", booking.getId().toString()) // <-- MÉTADONNÉE POUR LES SERVICES
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        logger.info("Created Stripe PaymentIntent {} for booking {}", paymentIntent.getId(), bookingId);
        return paymentIntent.getClientSecret();
    }

    @Transactional
    public HttpResponse<com.paypal.orders.Order> createPayPalOrderForBooking(Long bookingId) throws IOException {
        Booking booking = findBookingOrThrow(bookingId);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        AmountWithBreakdown amountBreakdown = new AmountWithBreakdown()
                .currencyCode("USD")
                .value(booking.getService().getPrice().toString());

        PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest()
                .amountWithBreakdown(amountBreakdown)
                .customId("booking-" + booking.getId().toString()); // <-- PRÉFIXE POUR LES SERVICES

        orderRequest.purchaseUnits(List.of(purchaseUnitRequest));

        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);
        HttpResponse<com.paypal.orders.Order> response = payPalHttpClient.execute(request);

        logger.info("Created PayPal Order {} for booking {}", response.result().id(), bookingId);
        return response;
    }


    // ============================================
    // == LES MÉTHODES DE VÉRIFICATION CI-DESSOUS SONT MAINTENANT PLUS GÉNÉRIQUES ==
    // ============================================

    public boolean verifyStripePayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(paymentIntent.getStatus());
        } catch (StripeException e) {
            logger.error("Failed to verify Stripe payment {}: {}", paymentIntentId, e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean capturePayPalOrder(String paypalOrderId) {
        try {
            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            HttpResponse<com.paypal.orders.Order> response = payPalHttpClient.execute(request);
            com.paypal.orders.Order capturedOrder = response.result();
            return "COMPLETED".equals(capturedOrder.status());
        } catch (IOException e) {
            logger.error("Failed to capture PayPal order {}: {}", paypalOrderId, e.getMessage());
            return false;
        }
    }

    // ============================
    // == MÉTHODES UTILITAIRES ====
    // ============================

    private Order findOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }

    // Nouvelle méthode utilitaire pour les bookings
    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
    }

    // Renommage pour plus de clarté
    private void createOrUpdatePaymentForOrder(Order order, String method) {
        paymentRepository.findByOrderId(order.getId())
                .map(payment -> {
                    payment.setPaymentMethod(method);
                    payment.setStatus(PaymentStatus.PENDING);
                    return paymentRepository.save(payment);
                })
                .orElseGet(() -> {
                    Payment newPayment = new Payment();
                    newPayment.setOrder(order);
                    newPayment.setPaymentMethod(method);
                    newPayment.setStatus(PaymentStatus.PENDING);
                    return paymentRepository.save(newPayment);
                });
    }

    // Cette méthode n'est plus utilisée pour la vérification, mais gardée si elle sert ailleurs
    @Transactional
    private void updatePaymentStatus(Long orderId, PaymentStatus status) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(status);
            paymentRepository.save(payment);
            logger.info("Updated payment status to {} for order {}", status, orderId);
        });
    }
}