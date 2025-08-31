package com.mediatower.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.cloud.Timestamp; // On importe le type de Firestore
import lombok.Data;

@Data
public class MessageDto {
    private String id;
    private String chatId;
    private String senderId;
    private String receiverId;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Timestamp timestamp;

    private boolean isRead;
    private boolean isAttachment;
}