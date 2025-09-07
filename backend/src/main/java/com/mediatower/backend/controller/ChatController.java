//package com.mediatower.backend.controller;
//
//import com.mediatower.backend.dto.ConversationDto;
//import com.mediatower.backend.dto.MessageDto;
//import com.mediatower.backend.dto.OfflineMessageRequest;
//import com.mediatower.backend.security.FirebaseUser;
//import com.mediatower.backend.service.ChatService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//
//import java.security.Principal;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/chats")
//public class ChatController {
//    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
//    private final ChatService chatService;
//
//    public ChatController(ChatService chatService) {
//        this.chatService = chatService;
//    }
//
//    @PostMapping("/offline-message")
//    public ResponseEntity<Void> receiveOfflineMessage(@RequestBody OfflineMessageRequest request) {
//        try {
//            chatService.saveOfflineMessage(request.getUserId(), request.getName(), request.getEmail(), request.getMessage());
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            logger.error("Error saving offline message", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @PostMapping("/send")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<?> sendMessage(@AuthenticationPrincipal FirebaseUser firebaseUser, @RequestBody MessageDto messageDto) {
//        try {
//            MessageDto sentMessage = chatService.sendMessage(firebaseUser.getUid(), messageDto);
//            return new ResponseEntity<>(sentMessage, HttpStatus.CREATED);
//        } catch (Exception e) {
//            logger.error("Error sending message for user {}", firebaseUser.getUid(), e);
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @GetMapping("/admin/inbox")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
//    public ResponseEntity<List<ConversationDto>> getAdminInbox() {
//        try {
//            List<ConversationDto> conversations = chatService.getAdminConversations();
//            return ResponseEntity.ok(conversations);
//        } catch (Exception e) {
//            logger.error("Error fetching admin inbox", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
//        }
//    }
//
//    @PostMapping("/{chatId}/mark-as-read")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
//    public ResponseEntity<Void> markAsRead(@PathVariable String chatId, @AuthenticationPrincipal FirebaseUser firebaseUser) {
//        try {
//            chatService.markConversationAsRead(chatId, firebaseUser.getUid());
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            logger.error("Error marking conversation {} as read", chatId, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @PostMapping("/{chatId}/close")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
//    public ResponseEntity<Void> closeConversation(@PathVariable String chatId) {
//        try {
//            chatService.closeConversation(chatId);
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            logger.error("Error closing conversation {}", chatId, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @PostMapping("/{chatId}/assign")
//    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
//    public ResponseEntity<Void> assignConversation(@PathVariable String chatId, @RequestBody Map<String, String> payload) {
//        try {
//            String adminUid = payload.get("adminUid");
//            if (adminUid == null || adminUid.isBlank()) {
//                return ResponseEntity.badRequest().build();
//            }
//            chatService.assignConversation(chatId, adminUid);
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            logger.error("Error assigning conversation {}", chatId, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    @GetMapping("/my-active-conversation")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, String>> getMyActiveConversation(@AuthenticationPrincipal FirebaseUser firebaseUser) {
//        try {
//            Optional<String> activeChatId = chatService.findActiveChatIdByUserId(firebaseUser.getUid());
//            return activeChatId
//                    .map(id -> ResponseEntity.ok(Map.of("chatId", id)))
//                    .orElseGet(() -> ResponseEntity.noContent().build());
//        } catch (Exception e) {
//            logger.error("Error fetching active conversation for user {}", firebaseUser.getUid(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//    @PostMapping("/{chatId}/mark-as-read-by-client")
//    public ResponseEntity<Void> markMessagesAsReadByClient(@PathVariable String chatId, Principal principal) {
//        // C'est la méthode correcte pour récupérer l'UID avec votre configuration
//        if (principal == null) {
//            // Sécurité : ne devrait jamais arriver si l'endpoint est bien sécurisé
//            return ResponseEntity.status(401).build();
//        }
//        String clientUid = principal.getName();
//        chatService.markMessagesAsReadByClient(chatId, clientUid);
//        return ResponseEntity.ok().build();
//    }
//}