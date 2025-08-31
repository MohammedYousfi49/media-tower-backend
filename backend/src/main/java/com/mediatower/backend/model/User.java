package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uid; // Firebase UID

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column
    private String lastName;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column
    private String verificationToken;

    @Column
    private LocalDateTime verificationTokenExpiryDate;

    @Column
    private String preferredLanguage;

    @Column
    private String phoneNumber;

    @Column
    private String address;

    @Column
    private String fcmToken;

    // === CHAMPS MFA ===
    @Column(nullable = false)
    private boolean mfaEnabled = false;

    @Column
    private String mfaSecret;

    @Column
    private String tempMfaSecret;

    // === CORRECTION CRITIQUE CI-DESSOUS ===
    // On force le chargement immédiat (EAGER) des codes pour éviter les erreurs de "LazyInitializationException".
    // C'est la cause principale des échecs de validation.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_recovery_codes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "recovery_code")
    private List<String> recoveryCodes;

    @Column
    private LocalDateTime recoveryCodesGeneratedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Méthodes utilitaires pour les codes de récupération
    public boolean hasRecoveryCodes() {
        return recoveryCodes != null && !recoveryCodes.isEmpty();
    }

    public int getAvailableRecoveryCodesCount() {
        return recoveryCodes != null ? recoveryCodes.size() : 0;
    }

    /**
     * Tente de consommer un code de récupération.
     * Le code est normalisé (majuscules, sans espaces/tirets) avant la comparaison
     * pour rendre la saisie utilisateur plus flexible.
     * @param providedCode Le code fourni par l'utilisateur.
     * @return true si le code a été trouvé et utilisé, false sinon.
     */
    public boolean useRecoveryCode(String providedCode) {
        if (recoveryCodes == null || providedCode == null) {
            return false;
        }

        // Normaliser le code fourni par l'utilisateur
        String normalizedProvidedCode = providedCode.trim().toUpperCase().replace("-", "");

        // Parcourir la liste pour trouver une correspondance normalisée
        String codeToReanove = null;
        for (String storedCode : recoveryCodes) {
            String normalizedStoredCode = storedCode.replace("-", "");
            if (normalizedStoredCode.equals(normalizedProvidedCode)) {
                codeToReanove = storedCode;
                break;
            }
        }

        if (codeToReanove != null) {
            return recoveryCodes.remove(codeToReanove);
        }

        return false;
    }
}