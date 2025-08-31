package com.mediatower.backend.model;

public enum BookingStatus {
    PENDING,      // Le client a fait une demande, en attente de validation par l'admin.
    PROCESSING,   // L'admin a vu la demande et s'est assigné la tâche (optionnel mais utile).
    CONFIRMED,    // L'admin a validé, en attente du paiement du client.
    IN_PROGRESS,  // Le client a payé, le travail a commencé.
    COMPLETED,    // Le travail est terminé.
    CANCELLED     // La réservation a été annulée (par l'admin ou automatiquement).
}