package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
// Ajout d'une contrainte pour s'assurer qu'un utilisateur n'a qu'un seul accès par produit
@Table(name = "user_product_access",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
public class UserProductAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    @Column(nullable = false)
    private int downloadCount = 0; // Compteur de téléchargements

    private LocalDateTime lastDownloadAt; // Date du dernier téléchargement

    private LocalDateTime accessExpiresAt; // Date d'expiration de l'accès (peut être null pour un accès à vie)

    // Constructeur pratique
    public UserProductAccess(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    // Méthode unique @PrePersist qui s'exécute avant de sauvegarder l'entité
    @PrePersist
    protected void onPurchase() {
        this.purchaseDate = LocalDateTime.now();
        // Par défaut, on donne un accès de 2 ans. Mettez null pour un accès illimité.
        this.accessExpiresAt = LocalDateTime.now().plusYears(2);
    }
}