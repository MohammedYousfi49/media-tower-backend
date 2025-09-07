//package com.mediatower.backend.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.simp.SimpMessageType;
//import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
//import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
//
//@Configuration
//public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
//
//    @Override
//    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
//        messages
//                // Autorise les messages de connexion, déconnexion et désinscription pour tous
//                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.DISCONNECT, SimpMessageType.UNSUBSCRIBE).permitAll()
//
//                // Un utilisateur authentifié peut envoyer des messages à l'application
//                .simpDestMatchers("/app/**").authenticated()
//
//                // Un utilisateur authentifié peut s'abonner aux topics
//                .simpSubscribeDestMatchers("/topic/**").authenticated()
//
//                // Refuse tout le reste
//                .anyMessage().denyAll();
//    }
//
//    @Override
//    protected boolean sameOriginDisabled() {
//        return true; // Important pour l'authentification par token
//    }
//}