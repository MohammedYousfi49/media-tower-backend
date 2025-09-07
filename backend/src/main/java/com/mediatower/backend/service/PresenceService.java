//// Fichier : src/main/java/com/mediatower/backend/service/PresenceService.java
//package com.mediatower.backend.service;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import java.util.Collections;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//public class PresenceService {
//
//    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);
//
//    private final Set<String> onlineAdminUids = ConcurrentHashMap.newKeySet();
//    private final Set<String> onlineClientUids = ConcurrentHashMap.newKeySet();
//    private final SimpMessagingTemplate messagingTemplate;
//
//    public PresenceService(SimpMessagingTemplate messagingTemplate) {
//        this.messagingTemplate = messagingTemplate;
//    }
//
//    public void adminConnected(String uid) {
//        if (onlineAdminUids.add(uid)) {
//            logger.info("Admin connected: {}. Total online admins: {}", uid, onlineAdminUids.size());
//            broadcastAdminStatusUpdate(); // --- MODIFIÉ ---
//        }
//    }
//
//    public void adminDisconnected(String uid) {
//        if (onlineAdminUids.remove(uid)) {
//            logger.info("Admin disconnected: {}. Total online admins: {}", uid, onlineAdminUids.size());
//            broadcastAdminStatusUpdate(); // --- MODIFIÉ ---
//        }
//    }
//
//    public void clientConnected(String uid) {
//        if (onlineClientUids.add(uid)) {
//            logger.info("Client connected: {}. Total online clients: {}", uid, onlineClientUids.size());
//            broadcastClientPresenceUpdate(); // --- MODIFIÉ ---
//        }
//    }
//
//    public void clientDisconnected(String uid) {
//        if (onlineClientUids.remove(uid)) {
//            logger.info("Client disconnected: {}. Total online clients: {}", uid, onlineClientUids.size());
//            broadcastClientPresenceUpdate(); // --- MODIFIÉ ---
//        }
//    }
//
//    public boolean isClientOnline(String uid) {
//        return uid != null && onlineClientUids.contains(uid);
//    }
//
//    public Set<String> getOnlineAdminUids() {
//        return Collections.unmodifiableSet(onlineAdminUids);
//    }
//
//    // --- AJOUT D'UNE MÉTHODE SPÉCIFIQUE POUR LE STATUT DES ADMINS ---
//    private void broadcastAdminStatusUpdate() {
//        boolean areAdminsAvailable = !onlineAdminUids.isEmpty();
//        logger.info("Broadcasting admin status. Available: {}", areAdminsAvailable);
//        // Topic public pour la ChatBubble
//        messagingTemplate.convertAndSend("/topic/support/status", Map.of("agentsAvailable", areAdminsAvailable));
//    }
//
//    // --- AJOUT D'UNE MÉTHODE SPÉCIFIQUE POUR LA PRÉSENCE DES CLIENTS ---
//    private void broadcastClientPresenceUpdate() {
//        Map<String, Boolean> onlineStatusMap = new ConcurrentHashMap<>();
//        onlineClientUids.forEach(uid -> onlineStatusMap.put(uid, true));
//
//        logger.info("Broadcasting client presence update. Online clients: {}", onlineClientUids.size());
//        // Topic pour l'inbox de l'admin
//        messagingTemplate.convertAndSend("/topic/presence", onlineStatusMap);
//    }
//}