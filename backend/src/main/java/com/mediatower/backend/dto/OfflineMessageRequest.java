package com.mediatower.backend.dto;

import lombok.Data;

@Data
public class OfflineMessageRequest {
    private String name;
    private String email;
    private String message;
    private String userId; // <-- AJOUTEZ CETTE LIGNE
}