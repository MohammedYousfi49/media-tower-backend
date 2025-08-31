package com.mediatower.backend.controller;

import com.mediatower.backend.dto.CreatePaymentRequestDto;
import com.mediatower.backend.dto.CreatePaymentResponseDto;
import com.mediatower.backend.model.BookingStatus;
import com.mediatower.backend.model.OrderStatus;
import com.mediatower.backend.service.BookingService;
import com.mediatower.backend.service.OrderService;
import com.mediatower.backend.service.PaymentService;
import com.paypal.http.HttpResponse;
import com.paypal.orders.Order;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final BookingService bookingService;

    public PaymentController(PaymentService paymentService, OrderService orderService, BookingService bookingService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.bookingService = bookingService;
    }

    // Endpoints pour les PRODUITS (Orders)
    @PostMapping("/stripe/create-payment-intent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreatePaymentResponseDto> createStripePaymentIntent(@RequestBody CreatePaymentRequestDto request) {
        try {
            String clientSecret = paymentService.createStripePaymentIntent(request.getOrderId());
            return ResponseEntity.ok(new CreatePaymentResponseDto(clientSecret, null));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/paypal/create-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreatePaymentResponseDto> createPayPalOrder(@RequestBody CreatePaymentRequestDto request) {
        try {
            HttpResponse<Order> response = paymentService.createPayPalOrder(request.getOrderId());
            String orderId = response.result().id();
            return ResponseEntity.ok(new CreatePaymentResponseDto(null, orderId));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoints pour les SERVICES (Bookings)
    @PostMapping("/stripe/create-booking-intent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreatePaymentResponseDto> createStripeBookingIntent(@RequestBody Map<String, Long> payload) {
        try {
            Long bookingId = payload.get("bookingId");
            if (bookingId == null) return ResponseEntity.badRequest().build();
            String clientSecret = paymentService.createStripePaymentIntentForBooking(bookingId);
            return ResponseEntity.ok(new CreatePaymentResponseDto(clientSecret, null));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/paypal/create-booking-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CreatePaymentResponseDto> createPayPalBookingOrder(@RequestBody Map<String, Long> payload) {
        try {
            Long bookingId = payload.get("bookingId");
            if (bookingId == null) return ResponseEntity.badRequest().build();
            HttpResponse<Order> response = paymentService.createPayPalOrderForBooking(bookingId);
            String payPalOrderId = response.result().id();
            return ResponseEntity.ok(new CreatePaymentResponseDto(null, payPalOrderId));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoints de confirmation manuelle (fallback)
    @PostMapping("/stripe/confirm-payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> confirmStripePayment(@RequestBody Map<String, String> request) {
        try {
            String paymentIntentId = request.get("paymentIntentId");
            if (paymentIntentId == null) return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "paymentIntentId is required"));

            if (paymentService.verifyStripePayment(paymentIntentId)) {
                if (request.containsKey("orderId")) {
                    Long orderId = Long.parseLong(request.get("orderId"));
                    orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                    return ResponseEntity.ok().body(Map.of("status", "success", "message", "Order confirmed"));
                } else if (request.containsKey("bookingId")) {
                    Long bookingId = Long.parseLong(request.get("bookingId"));
                    bookingService.confirmBookingPayment(bookingId);
                    return ResponseEntity.ok().body(Map.of("status", "success", "message", "Booking confirmed"));
                }
            }

            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Payment not successful or invalid request"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Failed to confirm payment"));
        }
    }

    @PostMapping("/paypal/capture-order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> capturePayPalOrder(@RequestBody Map<String, String> request) {
        try {
            String paypalOrderId = request.get("paypalOrderId");
            if (paypalOrderId == null) return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "paypalOrderId is required"));

            // ====================== CORRECTION DE L'ERREUR ======================
            // La méthode capturePayPalOrder dans PaymentService n'attend plus qu'un seul argument.
            boolean captureSuccess = paymentService.capturePayPalOrder(paypalOrderId);
            // ====================================================================

            if (captureSuccess) {
                if (request.containsKey("orderId")) {
                    Long orderId = Long.parseLong(request.get("orderId"));
                    orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                    return ResponseEntity.ok().body(Map.of("status", "success", "message", "Payment captured and order confirmed"));
                } else if (request.containsKey("bookingId")) {
                    Long bookingId = Long.parseLong(request.get("bookingId"));
                    bookingService.confirmBookingPayment(bookingId);
                    return ResponseEntity.ok().body(Map.of("status", "success", "message", "Payment captured and booking confirmed"));
                }
            }

            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Failed to capture payment or invalid request"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Failed to process payment"));
        }
    }

    // Endpoint de vérification de statut (pour les produits seulement)
    @GetMapping("/order-status/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOrderStatus(@PathVariable Long orderId) {
        try {
            var order = orderService.getOrderById(orderId);
            return ResponseEntity.ok().body(Map.of(
                    "orderId", orderId,
                    "status", order.getStatus(),
                    "totalAmount", order.getTotalAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}