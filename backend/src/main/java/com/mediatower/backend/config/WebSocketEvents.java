//package com.mediatower.backend.config;
//
//import com.mediatower.backend.model.UserRole;
//import com.mediatower.backend.security.FirebaseUser;
//import com.mediatower.backend.service.PresenceService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.event.EventListener;
//import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.messaging.SessionConnectEvent;
//import org.springframework.web.socket.messaging.SessionDisconnectEvent;
//import org.springframework.web.socket.messaging.SessionSubscribeEvent;
//
//import java.security.Principal;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//public class WebSocketEvents {
//
//    private static final Logger logger = LoggerFactory.getLogger(WebSocketEvents.class);
//    private final PresenceService presenceService;
//
//    // Map pour lier l'ID de session WebSocket à l'UID de l'utilisateur
//    private final Map<String, String> sessionToUidMap = new ConcurrentHashMap<>();
//
//    @Autowired
//    public WebSocketEvents(PresenceService presenceService) {
//        this.presenceService = presenceService;
//    }
//
//    // Cet événement se déclenche dès qu'une connexion physique est établie.
//    // L'utilisateur n'est pas encore authentifié à ce stade.
//    @EventListener
//    public void handleSessionConnected(SessionConnectEvent event) {
//        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
//        String sessionId = headers.getSessionId();
//        logger.info("WebSocket Session Connected: {}", sessionId);
//    }
//
//    // Cet événement se déclenche APRÈS l'authentification, quand le client s'abonne à un topic.
//    // C'est le moment le plus fiable pour récupérer l'utilisateur.
//    @EventListener
//    public void handleSessionSubscribe(SessionSubscribeEvent event) {
//        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
//        String sessionId = headers.getSessionId();
//        Principal userPrincipal = headers.getUser();
//
//        if (userPrincipal != null && !sessionToUidMap.containsKey(sessionId)) {
//            if (userPrincipal instanceof Authentication &&
//                    ((Authentication) userPrincipal).getPrincipal() instanceof FirebaseUser) {
//
//                FirebaseUser user = (FirebaseUser) ((Authentication) userPrincipal).getPrincipal();
//                String uid = user.getUid();
//                sessionToUidMap.put(sessionId, uid);
//
//                // Log pour debug
//                logger.info("User {} subscribed with role: {}", uid, user.getRole());
//
//                if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SELLER) {
//                    presenceService.adminConnected(uid);
//                    logger.info("Admin {} marked as online", uid);
//                } else {
//                    presenceService.clientConnected(uid);
//                    logger.info("Client {} marked as online", uid);
//                }
//            }
//        }
//    }
//
//    // Cet événement se déclenche à la déconnexion.
//    @EventListener
//    public void handleSessionDisconnect(SessionDisconnectEvent event) {
//        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
//        String sessionId = headers.getSessionId();
//
//        // On récupère l'UID qui était associé à cette session
//        String uid = sessionToUidMap.remove(sessionId);
//
//        if (uid != null) {
//            logger.info("WebSocket Session Disconnected: {} for UID: {}", sessionId, uid);
//
//            // On vérifie le rôle pour savoir s'il s'agit d'un admin ou d'un client
//            // Note: Ici, nous n'avons plus l'objet User complet, donc nous devons déconnecter les deux au cas où.
//            // PresenceService est conçu pour gérer cela sans erreur.
//            presenceService.adminDisconnected(uid);
//            presenceService.clientDisconnected(uid);
//        } else {
//            logger.warn("No UID found for disconnected session: {}", sessionId);
//        }
//    }
//}