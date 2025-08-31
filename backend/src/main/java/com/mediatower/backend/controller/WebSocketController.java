package com.mediatower.backend.controller;

import com.mediatower.backend.dto.AdminUserDto;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.PresenceService;
import com.mediatower.backend.service.UserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final UserService userService;

    public WebSocketController(SimpMessagingTemplate messagingTemplate, PresenceService presenceService, UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
        this.userService = userService;
    }

    @MessageMapping("/chat/{chatId}/typing")
    public void handleTypingEvent(
            @DestinationVariable String chatId,
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        if (authentication == null || !(authentication.getPrincipal() instanceof FirebaseUser)) {
            return;
        }
        FirebaseUser user = (FirebaseUser) authentication.getPrincipal();

        String isTyping = payload.get("isTyping");
        if (isTyping == null) { return; }

        Map<String, String> typingStatus = Map.of("isTyping", isTyping, "senderUid", user.getUid());
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, typingStatus);
    }

    @MessageMapping("/support/status")
    @SendToUser("/queue/support-status")
    public List<AdminUserDto> getSupportStatus() {
        return presenceService.getOnlineAdminUids().stream()
                .flatMap(uid -> userService.findUserByUid(uid).stream())
                .map(userService::convertToAdminDto)
                .collect(Collectors.toList());
    }

    // La méthode handlePresenceConnect a été retirée car la logique est maintenant dans WebSocketEvents.java
}