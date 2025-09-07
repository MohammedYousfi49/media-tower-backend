// Chemin : src/main/java/com/mediatower/backend/model/Notification.java
package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
// =========== CORRECTION : Remplacer @Data par @Getter et @Setter ===========
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // nullable = true est le défaut, donc on peut l'enlever
    private User user;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String type;

    // Renommé pour correspondre aux conventions Java (isRead -> read)
    // Lombok génèrera bien un getter isRead() pour un boolean
    @Column(nullable = false)
    private boolean read;

    private LocalDateTime createdAt;
    @Column(length = 255) // On peut définir une longueur
    private String link;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}