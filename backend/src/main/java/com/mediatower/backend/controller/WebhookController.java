package com.mediatower.backend.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediatower.backend.model.OrderStatus;
import com.mediatower.backend.service.BookingService;
import com.mediatower.backend.service.OrderService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final OrderService orderService;
    private final BookingService bookingService;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    public WebhookController(OrderService orderService, BookingService bookingService) {
        this.orderService = orderService;
        this.bookingService = bookingService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            logger.warn("Stripe webhook signature verification failed.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                Map<String, String> metadata = paymentIntent.getMetadata();
                if (metadata.containsKey("orderId")) {
                    Long orderId = Long.parseLong(metadata.get("orderId"));
                    logger.info("[Stripe] Succeeded webhook for PRODUCT received. Updating orderId: {}", orderId);
                    orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                } else if (metadata.containsKey("bookingId")) {
                    Long bookingId = Long.parseLong(metadata.get("bookingId"));
                    logger.info("[Stripe] Succeeded webhook for SERVICE received. Confirming bookingId: {}", bookingId);
                    bookingService.confirmBookingPayment(bookingId);
                }
            }
        }
        return ResponseEntity.ok("Received");
    }

    @PostMapping("/paypal")
    public ResponseEntity<String> handlePayPalWebhook(@RequestBody String payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Map<String, Object> webhookEvent = mapper.readValue(payload, Map.class);
            String eventType = (String) webhookEvent.get("event_type");

            if ("CHECKOUT.ORDER.APPROVED".equals(eventType)) {
                Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
                java.util.List<Map<String, Object>> purchaseUnits = (java.util.List<Map<String, Object>>) resource.get("purchase_units");

                if (purchaseUnits != null && !purchaseUnits.isEmpty()) {
                    String customId = (String) purchaseUnits.get(0).get("custom_id");
                    if (customId != null && !customId.isEmpty()) {
                        if (customId.startsWith("order-")) {
                            Long orderId = Long.parseLong(customId.substring(6));
                            logger.info("[PayPal] Webhook for PRODUCT received for approved orderId: {}", orderId);
                            orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                        } else if (customId.startsWith("booking-")) {
                            Long bookingId = Long.parseLong(customId.substring(8));
                            logger.info("[PayPal] Webhook for SERVICE received for approved bookingId: {}", bookingId);
                            bookingService.confirmBookingPayment(bookingId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error processing PayPal webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok("Received");
    }
}