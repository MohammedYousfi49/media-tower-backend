//package com.mediatower.backend.dto;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.util.Date; // Changez 'Object' en 'Date' pour une meilleure gestion
//import java.util.Map;
//
//@Data
//@NoArgsConstructor
//public class ConversationDto {
//    private String chatId;
//    private ClientInfo client;
//    private String lastMessage;
//
//    // Changez le type 'Object' en 'Date' ou 'Instant' pour une meilleure sérialisation
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
//    private Date lastMessageTimestamp;
//
//    private String status;
//    private String assignedAdminName;
//    private int unreadCount;
//
//    private Map<String, String> initialMessageMetadata; // Ce champ semble être une ancienne tentative, vous pouvez le garder ou le supprimer
//
//    @Data
//    @NoArgsConstructor
//    public static class ClientInfo {
//        private String id;
//        private String name;
//        private String email; // <-- AJOUTEZ CE CHAMP
//        private boolean isOnline;
//    }
//}