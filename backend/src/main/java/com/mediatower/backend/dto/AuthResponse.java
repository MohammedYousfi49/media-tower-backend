package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String firebaseToken; // Token d'ID Firebase après connexion
    private String email;
    private String role; // Rôle de l'utilisateur (USER, ADMIN, SELLER)
    private Long userId;
}