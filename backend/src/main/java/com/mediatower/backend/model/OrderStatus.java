package com.mediatower.backend.model;

public enum OrderStatus {
    PENDING,    // En attente de confirmation/paiement
    CONFIRMED,  // Confirmée et payée
    PROCESSING, // En cours de préparation/traitement
    SHIPPED,    // Expédiée (pour les produits physiques, même si ce sont des numériques, peut signifier "envoyé par email")
    DELIVERED,  // Livrée/Accès accordé
    CANCELLED,  // Annulée
    REFUNDED    // Remboursée
}