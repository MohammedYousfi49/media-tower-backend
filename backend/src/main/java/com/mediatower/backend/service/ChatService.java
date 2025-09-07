//package com.mediatower.backend.service;
//
//import com.google.api.core.ApiFuture;
//import com.google.cloud.firestore.*;
//import com.mediatower.backend.dto.ConversationDto;
//import com.mediatower.backend.dto.MessageDto;
//import com.mediatower.backend.model.User;
//import com.mediatower.backend.model.UserRole;
//import com.mediatower.backend.repository.UserRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.concurrent.ExecutionException;
//import java.util.stream.Collectors;
//
//@Service
//@SuppressWarnings("unchecked")
//public class ChatService {
//    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
//    private static final String OFFLINE_SUPPORT_UID = "default_support_uid";
//
//    private final Firestore firestore;
//    private final UserRepository userRepository;
//    private final SimpMessagingTemplate messagingTemplate;
//    private final PresenceService presenceService;
//    private final PushNotificationService pushNotificationService;
//
//    public ChatService(Firestore firestore, UserRepository userRepository, SimpMessagingTemplate messagingTemplate, PushNotificationService pushNotificationService, PresenceService presenceService) {
//        this.firestore = firestore;
//        this.userRepository = userRepository;
//        this.messagingTemplate = messagingTemplate;
//        this.pushNotificationService = pushNotificationService;
//        this.presenceService = presenceService;
//    }
//
//    @Transactional
//    public void saveOfflineMessage(String userUid, String name, String email, String message) throws ExecutionException, InterruptedException {
//        String visitorId = (userUid != null) ? userUid : "visitor_" + email;
//        String chatId = getChatId(visitorId, OFFLINE_SUPPORT_UID);
//        DocumentReference chatRef = firestore.collection("chats").document(chatId);
//        chatRef.set(Map.of(
//                "participant_uids", List.of(visitorId, OFFLINE_SUPPORT_UID),
//                "participant_names", Map.of(visitorId, name, OFFLINE_SUPPORT_UID, "Support"),
//                "status", "OPEN",
//                "lastActivity", FieldValue.serverTimestamp()
//        ), SetOptions.merge()).get();
//        chatRef.collection("messages").add(Map.of(
//                "content", String.format("Message de %s (%s):\n\n%s", name, email, message),
//                "senderId", visitorId,
//                "timestamp", FieldValue.serverTimestamp(),
//                "isRead", false
//        )).get();
//        notifyAdminInbox(chatId);
//    }
//
//    // Dans ChatService.java
//
//    @Transactional
//    public MessageDto sendMessage(String senderId, MessageDto messageDto) throws ExecutionException, InterruptedException {
//        // 1. Valider l'expéditeur et récupérer ses informations
//        User sender = userRepository.findByUid(senderId)
//                .orElseThrow(() -> new RuntimeException("Sender not found with UID: " + senderId));
//
//        // 2. Obtenir les références Firestore
//        String chatId = getChatId(senderId, messageDto.getReceiverId());
//        DocumentReference chatRef = firestore.collection("chats").document(chatId);
//
//        // 3. Construire la Map de données pour Firestore - C'est la correction principale
//        // On n'utilise plus le DTO directement pour éviter les erreurs de sérialisation.
//        Map<String, Object> messageData = new HashMap<>();
//        messageData.put("content", messageDto.getContent());
//        messageData.put("senderId", senderId); // On utilise l'ID authentifié pour la sécurité
//        messageData.put("receiverId", messageDto.getReceiverId());
//        messageData.put("isRead", false);
//        // On demande à Firestore de générer le timestamp sur le serveur. C'est la méthode la plus fiable.
//        messageData.put("timestamp", FieldValue.serverTimestamp());
//
//        // 4. Enregistrer le nouveau message dans Firestore
//        DocumentReference messageRef = chatRef.collection("messages").add(messageData).get();
//
//        // Mettre à jour l'ID dans le DTO pour le renvoyer au client et au WebSocket
//        messageDto.setId(messageRef.getId());
//
//        // 5. Mettre à jour le document de conversation principal
//        chatRef.update("lastActivity", FieldValue.serverTimestamp(), "status", "OPEN").get();
//
//        // 6. Diffuser le message complet via WebSocket pour une expérience temps réel
//        messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageDto);
//
//        // 7. Gérer les notifications push pour les utilisateurs hors ligne
//        userRepository.findByUid(messageDto.getReceiverId()).ifPresent(receiver -> {
//            boolean isClientOnline = presenceService.isClientOnline(receiver.getUid());
//            if (!isClientOnline && receiver.getFcmToken() != null) {
//                pushNotificationService.sendPushNotificationToToken(
//                        receiver.getFcmToken(),
//                        "Nouveau message de " + sender.getFirstName(), // Personnalisation possible
//                        messageDto.getContent()
//                );
//            }
//        });
//
//        // 8. Mettre à jour la liste des conversations des administrateurs
//        notifyAdminInbox(chatId);
//
//        // Renvoyer le DTO complet, y compris l'ID généré
//        return messageDto;
//    }
//
//    @Transactional
//    public void markConversationAsRead(String chatId, String adminUid) {
//        try {
//            WriteBatch batch = firestore.batch();
//            QuerySnapshot querySnapshot = firestore.collection("chats").document(chatId).collection("messages")
//                    .whereEqualTo("isRead", false).whereNotEqualTo("senderId", adminUid).get().get();
//            if (querySnapshot.isEmpty()) return;
//            querySnapshot.getDocuments().forEach(doc -> batch.update(doc.getReference(), "isRead", true));
//            batch.commit().get();
//        } catch (Exception e) {
//            logger.error("Error marking admin messages as read for chat {}", chatId, e);
//        }
//    }
//
//    @Transactional
//    public void closeConversation(String chatId) {
//        firestore.collection("chats").document(chatId).update("status", "CLOSED");
//        notifyAdminInbox(chatId);
//    }
//
//    @Transactional
//    public void assignConversation(String chatId, String adminUid) {
//        firestore.collection("chats").document(chatId).update("assignedTo", adminUid);
//        notifyAdminInbox(chatId);
//    }
//
//    @Transactional(readOnly = true)
//    public List<ConversationDto> getAdminConversations() throws ExecutionException, InterruptedException {
//        List<QueryDocumentSnapshot> documents = firestore.collection("chats")
//                .orderBy("lastActivity", Query.Direction.DESCENDING).get().get().getDocuments();
//        Map<String, User> userMap = userRepository.findAll().stream().filter(u -> u.getUid() != null)
//                .collect(Collectors.toMap(User::getUid, u -> u, (u1, u2) -> u1));
//        List<ConversationDto> conversations = new ArrayList<>();
//        for (QueryDocumentSnapshot doc : documents) {
//            buildConversationDto(doc, userMap).ifPresent(conversations::add);
//        }
//        return conversations;
//    }
//
//    @Transactional(readOnly = true)
//    public Optional<String> findActiveChatIdByUserId(String userId) throws ExecutionException, InterruptedException {
//        QuerySnapshot querySnapshot = firestore.collection("chats")
//                .whereArrayContains("participant_uids", userId).whereEqualTo("status", "OPEN").limit(1).get().get();
//        return querySnapshot.getDocuments().stream().findFirst().map(DocumentSnapshot::getId);
//    }
//
//    @Transactional
//    public void markMessagesAsReadByClient(String chatId, String clientUid) {
//        logger.info("Marking messages as read by client {} for chat {}", clientUid, chatId);
//        try {
//            WriteBatch batch = firestore.batch();
//            QuerySnapshot querySnapshot = firestore.collection("chats").document(chatId).collection("messages")
//                    .whereNotEqualTo("senderId", clientUid).whereEqualTo("isRead", false).get().get();
//            if (querySnapshot.isEmpty()) return;
//            List<String> messageIds = new ArrayList<>();
//            querySnapshot.forEach(doc -> {
//                batch.update(doc.getReference(), "isRead", true);
//                messageIds.add(doc.getId());
//            });
//            batch.commit().get();
//            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/read-receipt", Map.of("readMessageIds", messageIds));
//        } catch (Exception e) {
//            logger.error("Error marking messages as read by client for chat {}", chatId, e);
//        }
//    }
//
//    private String getChatId(String userId1, String userId2) {
//        return userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
//    }
//
//    private void notifyAdminInbox(String chatId) {
//        Map<String, User> userMap = userRepository.findAll().stream().filter(u -> u.getUid() != null)
//                .collect(Collectors.toMap(User::getUid, u -> u, (u1, u2) -> u1));
//        try {
//            DocumentSnapshot doc = firestore.collection("chats").document(chatId).get().get();
//            buildConversationDto(doc, userMap).ifPresent(dto -> messagingTemplate.convertAndSend("/topic/admin/inbox/update", dto));
//        } catch (Exception e) {
//            logger.error("Could not build or send DTO for chat {}", chatId, e);
//        }
//    }
//
//    private Optional<ConversationDto> buildConversationDto(DocumentSnapshot doc, Map<String, User> userMap) {
//        if (!doc.exists()) return Optional.empty();
//        try {
//            List<String> participantUids = (List<String>) doc.get("participant_uids");
//            if (participantUids == null) return Optional.empty();
//
//            String clientUid = participantUids.stream()
//                    .filter(uid -> !uid.equals(OFFLINE_SUPPORT_UID)) // Correction de la syntaxe
//                    .filter(uid -> !userMap.containsKey(uid) || userMap.get(uid).getRole() == UserRole.USER)
//                    .findFirst()
//                    .orElse(null);
//
//            if (clientUid == null) return Optional.empty();
//
//            User clientUser = userMap.get(clientUid);
//            String clientName, clientEmail;
//            if (clientUser != null) {
//                String firstName = clientUser.getFirstName() == null ? "" : clientUser.getFirstName();
//                String lastName = clientUser.getLastName() == null ? "" : clientUser.getLastName();
//                clientName = (firstName + " " + lastName).trim();
//                clientEmail = clientUser.getEmail();
//                if (clientName.isEmpty() && clientEmail != null) {
//                    clientName = clientEmail.split("@")[0];
//                }
//            } else {
//                Map<String, String> namesMap = (Map<String, String>) doc.get("participant_names");
//                clientName = (namesMap != null) ? namesMap.get(clientUid) : "Visiteur";
//                clientEmail = clientUid.startsWith("visitor_") ? clientUid.substring("visitor_".length()) : "Email inconnu";
//            }
//            if (clientName == null || clientName.isEmpty()) clientName = "Utilisateur";
//
//            ConversationDto dto = new ConversationDto();
//            dto.setChatId(doc.getId());
//            ConversationDto.ClientInfo clientInfo = new ConversationDto.ClientInfo();
//            clientInfo.setId(clientUid);
//            clientInfo.setName(clientName);
//            clientInfo.setEmail(clientEmail);
//            clientInfo.setOnline(presenceService.isClientOnline(clientUid));
//            dto.setClient(clientInfo);
//
//            dto.setStatus(doc.getString("status") != null ? doc.getString("status") : "OPEN");
//            String assignedToUid = doc.getString("assignedTo");
//            if (assignedToUid != null && userMap.containsKey(assignedToUid)) {
//                dto.setAssignedAdminName(userMap.get(assignedToUid).getFirstName());
//            }
//
//            QuerySnapshot lastMessageSnapshot = doc.getReference().collection("messages").orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().get();
//            if (!lastMessageSnapshot.isEmpty()) {
//                DocumentSnapshot lastMessageDoc = lastMessageSnapshot.getDocuments().get(0);
//                dto.setLastMessage(lastMessageDoc.getString("content"));
//                dto.setLastMessageTimestamp(lastMessageDoc.getDate("timestamp"));
//            } else {
//                dto.setLastMessage("Pas de messages.");
//                dto.setLastMessageTimestamp(doc.getDate("lastActivity"));
//            }
//
//            QuerySnapshot unreadSnapshot = doc.getReference().collection("messages").whereEqualTo("isRead", false).whereEqualTo("senderId", clientUid).get().get();
//            dto.setUnreadCount(unreadSnapshot.size());
//
//            return Optional.of(dto);
//        } catch (Exception e) {
//            logger.error("Failed to build DTO for chat doc {}", doc.getId(), e);
//            return Optional.empty();
//        }
//    }
//}