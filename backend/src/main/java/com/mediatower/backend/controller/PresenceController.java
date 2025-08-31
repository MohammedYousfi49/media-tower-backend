package com.mediatower.backend.controller;

import com.mediatower.backend.model.UserRole;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

//package com.mediatower.backend.controller;
//
//import com.mediatower.backend.model.UserRole;
//import com.mediatower.backend.security.FirebaseUser;
//import com.mediatower.backend.service.PresenceService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.event.EventListener;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.socket.messaging.SessionConnectedEvent;
//import org.springframework.web.socket.messaging.SessionDisconnectEvent;
//
@Controller
public class PresenceController {

    private static final Logger logger = LoggerFactory.getLogger(PresenceController.class);
    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    //
//    @EventListener
//    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
//        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
//        Authentication authentication = (Authentication) headers.getUser();
//
//        if (authentication != null && authentication.getPrincipal() instanceof FirebaseUser) {
//            FirebaseUser firebaseUser = (FirebaseUser) authentication.getPrincipal();
//            if (firebaseUser.getRole() == UserRole.ADMIN || firebaseUser.getRole() == UserRole.SELLER) {
//                presenceService.adminConnected(firebaseUser.getUid());
//            } else {
//                presenceService.clientConnected(firebaseUser.getUid());
//            }
//        }
//    }
//
//    @EventListener
//    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
//        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
//        Authentication authentication = (Authentication) headers.getUser();
//
//        if (authentication != null && authentication.getPrincipal() instanceof FirebaseUser) {
//            FirebaseUser firebaseUser = (FirebaseUser) authentication.getPrincipal();
//            if (firebaseUser.getRole() == UserRole.ADMIN || firebaseUser.getRole() == UserRole.SELLER) {
//                presenceService.adminDisconnected(firebaseUser.getUid());
//            } else {
//                presenceService.clientDisconnected(firebaseUser.getUid());
//            }
//        }
//    }
//}
    @PostMapping("/api/presence/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> sendHeartbeat(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        if (firebaseUser.getRole() == UserRole.ADMIN || firebaseUser.getRole() == UserRole.SELLER) {
            presenceService.adminConnected(firebaseUser.getUid());
        } else {
            presenceService.clientConnected(firebaseUser.getUid());
        }
        return ResponseEntity.ok().build();
    }
}