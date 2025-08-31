// Chemin : src/main/java/com/mediatower/backend/config/PaymentConfig.java
package com.mediatower.backend.config;

import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConfig {

    @Value("${stripe.api.secret-key}")
    private String stripeSecretKey;

    @Value("${paypal.client.id}") // CORRECTION : Utilisation de points pour être cohérent
    private String paypalClientId;

    @Value("${paypal.client.secret}") // CORRECTION : Utilisation de points pour être cohérent
    private String paypalClientSecret;

    @Value("${paypal.mode}")
    private String paypalMode;

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Bean
    public PayPalHttpClient payPalHttpClient() {
        PayPalEnvironment environment = "live".equals(paypalMode)
                ? new PayPalEnvironment.Live(paypalClientId, paypalClientSecret)
                : new PayPalEnvironment.Sandbox(paypalClientId, paypalClientSecret);
        return new PayPalHttpClient(environment);
    }
}