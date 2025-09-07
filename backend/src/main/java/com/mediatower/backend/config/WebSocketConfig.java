//// Fichier : src/main/java/com/mediatower/backend/config/WebSocketConfig.java (COMPLET ET FINAL)
//
//package com.mediatower.backend.config;
//
//import com.mediatower.backend.security.AuthChannelInterceptor; // <-- AJOUT
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.simp.config.ChannelRegistration; // <-- AJOUT
//import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
//
//@Configuration
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//
//    // --- DÉBUT DE L'AJOUT ---
//    private final AuthChannelInterceptor authChannelInterceptor;
//
//    public WebSocketConfig(AuthChannelInterceptor authChannelInterceptor) {
//        this.authChannelInterceptor = authChannelInterceptor;
//    }
//    // --- FIN DE L'AJOUT ---
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry) {
//        registry.enableSimpleBroker("/topic");
//        registry.setApplicationDestinationPrefixes("/app");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        // --- MODIFICATION : On utilise setAllowedOriginPatterns("*") pour une meilleure compatibilité ---
//        registry.addEndpoint("/ws")
//                .setAllowedOriginPatterns("*") // Permet toutes les origines, plus flexible pour le développement
//                .withSockJS();
//    }
//
//    // --- DÉBUT DE L'AJOUT DE LA NOUVELLE MÉTHODE ---
//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        // On enregistre notre intercepteur pour qu'il s'exécute sur chaque message entrant
//        registration.interceptors(authChannelInterceptor);
//    }
//    // --- FIN DE L'AJOUT ---
//}