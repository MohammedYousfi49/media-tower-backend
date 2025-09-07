//package com.mediatower.backend.security;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseToken;
//import com.mediatower.backend.model.User;
//import com.mediatower.backend.service.UserService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageChannel;
//import org.springframework.messaging.simp.stomp.StompCommand;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.messaging.support.ChannelInterceptor;
//import org.springframework.messaging.support.MessageHeaderAccessor;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//
//@Component
//public class AuthChannelInterceptor implements ChannelInterceptor {
//
//    private static final Logger logger = LoggerFactory.getLogger(AuthChannelInterceptor.class);
//    private final UserService userService;
//
//    public AuthChannelInterceptor(UserService userService) {
//        this.userService = userService;
//    }
//
//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//
//        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
//            String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
//
//            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
//                String token = authorizationHeader.substring(7);
//                try {
//                    FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
//
//                    // --- CORRECTION MAJEURE : Utilisation de la nouvelle méthode findUserByToken ---
//                    Optional<User> userOptional = userService.findUserByToken(decodedToken);
//
//                    if (userOptional.isPresent()) {
//                        User userInDb = userOptional.get();
//                        FirebaseUser firebaseUser = FirebaseUser.create(userInDb);
//                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                                firebaseUser, null, firebaseUser.getAuthorities());
//
//                        accessor.setUser(authentication);
//                        logger.info("WebSocket user authenticated: {}", firebaseUser.getEmail());
//                    } else {
//                        logger.warn("WebSocket connection attempt with a valid Firebase token, but the user was not found in our database: {}", decodedToken.getEmail());
//                    }
//                } catch (Exception e) {
//                    logger.error("WebSocket authentication failed due to an invalid token.", e);
//                }
//            } else {
//                logger.warn("WebSocket connection attempt without an Authorization header.");
//            }
//        }
//        return message;
//    }
//}