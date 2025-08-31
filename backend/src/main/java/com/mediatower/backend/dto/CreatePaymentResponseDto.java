package com.mediatower.backend.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatePaymentResponseDto {
    private String clientSecret; // Pour Stripe
    private String orderId;      // Pour PayPal
}