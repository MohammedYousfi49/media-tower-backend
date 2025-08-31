package com.mediatower.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long id;
    private Long orderId;
    private String paymentMethod; // STRIPE, PAYPAL, COD
    private String transactionId; // PaymentIntent ID (Stripe), Payment ID (PayPal), ou null (COD)
    private String status; // PENDING, COMPLETED, FAILED
    private String redirectUrl; // Pour PayPal
}