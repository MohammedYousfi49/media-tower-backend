package com.mediatower.backend.model;

public enum PasswordChangeMethod {
    BY_USER,        // Changé par l'utilisateur lui-même dans ses paramètres
    BY_RESET_LINK   // Réinitialisé via le lien "mot de passe oublié"
}