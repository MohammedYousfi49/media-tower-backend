package com.mediatower.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.AuditLogRepository;
import com.mediatower.backend.repository.PasswordHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import com.mediatower.backend.dto.LoginHistoryDto;


import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.mediatower.backend.dto.AdminUserDto;
import com.mediatower.backend.dto.RegisterRequest;
import com.mediatower.backend.dto.UserProfileDto;
import com.mediatower.backend.repository.PasswordResetTokenRepository;
import com.mediatower.backend.repository.UserRepository;
import com.mediatower.backend.dto.PasswordHistoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;
    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetTokenRepository tokenRepository;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;
    private final AuditLogRepository auditLogRepository;



    public UserService(UserRepository userRepository, PasswordHistoryRepository passwordHistoryRepository, PasswordEncoder passwordEncoder, EmailService emailService, PasswordResetTokenRepository tokenRepository, AuditLogService auditLogService, FileStorageService fileStorageService, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenRepository = tokenRepository;
        this.auditLogService = auditLogService;
        this.fileStorageService = fileStorageService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public User processRegistration(RegisterRequest registerRequest) {
        logger.info("Processing registration for email: {}", registerRequest.getEmail());

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("Registration failed: Email already exists - {}", registerRequest.getEmail());
            throw new IllegalStateException("Email is already associated with another account.");
        }
        if (registerRequest.getUid() != null && !registerRequest.getUid().isBlank()) {
            Optional<User> existingUserWithUid = userRepository.findByUid(registerRequest.getUid());
            if (existingUserWithUid.isPresent()) {
                logger.warn("Registration failed: UID already exists - {}", registerRequest.getUid());
                throw new IllegalStateException("User with this Firebase UID is already registered.");
            }
        }

        User newUser = new User();
        newUser.setUid(registerRequest.getUid());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setFirstName(registerRequest.getFirstName());
        newUser.setLastName(registerRequest.getLastName());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setRole(UserRole.USER); // Assurez-vous que le rôle USER est toujours attribué
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setEmailVerified(false);
        String token = UUID.randomUUID().toString();
        newUser.setVerificationToken(token);
        newUser.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(newUser);
        logger.info("User successfully registered: {}", savedUser.getEmail());

        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), token);
            logger.info("Verification email sent to: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", savedUser.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send verification email after registration. Please try again.");
        }

        return savedUser;
    }

    @Transactional
    public void verifyUser(String token) {
        logger.info("Attempting to verify user with token: {}", token);

        Optional<User> userOptional = userRepository.findByVerificationToken(token);

        if (userOptional.isEmpty()) {
            logger.warn("Verification failed: Token not found - {}", token);
            throw new IllegalStateException("Invalid verification token. It might be expired or already used.");
        }

        User user = userOptional.get();
        logger.info("Found user {} with token. Checking expiry date.", user.getEmail());

        if (user.getVerificationTokenExpiryDate() == null ||
                user.getVerificationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Verification failed for user {}: Token has expired.", user.getEmail());
            throw new IllegalStateException("Verification token has expired.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiryDate(null);
        userRepository.save(user);

        logger.info("User email {} successfully verified.", user.getEmail());
    }
    @Transactional(readOnly = true)
    public List<LoginHistoryDto> getLoginHistoryForUser(User user) {
        // On récupère les deux types de connexions réussies
        List<AuditLog> successfulLogins = auditLogRepository.findTop10ByUserAndActionOrderByTimestampDesc(user, SecurityActionType.LOGIN_SUCCESS);
        List<AuditLog> successfulMfa = auditLogRepository.findTop10ByUserAndActionOrderByTimestampDesc(user, SecurityActionType.MFA_VERIFICATION_SUCCESS);

        // On fusionne et trie pour avoir les plus récents
        List<AuditLog> combinedHistory = new java.util.ArrayList<>();
        combinedHistory.addAll(successfulLogins);
        combinedHistory.addAll(successfulMfa);

        combinedHistory.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return combinedHistory.stream()
                .limit(10) // On garde les 10 plus récents au total
                .map(this::convertToLoginHistoryDto)
                .collect(Collectors.toList());
    }
    private LoginHistoryDto convertToLoginHistoryDto(AuditLog log) {
        return new LoginHistoryDto(
                log.getTimestamp(),
                log.getIpAddress(),
                log.getDetails()
        );
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        logger.info("Resend verification requested for email: {}", email);

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            logger.info("Resend verification requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();

        if (user.isEmailVerified()) {
            logger.warn("Resend verification requested for already verified email: {}", email);
            throw new IllegalStateException("Your email is already verified. Please log in.");
        }

        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), newToken);
            logger.info("New verification email sent to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to resend verification email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again later.");
        }
    }

    @Transactional
    public User findOrCreateUserFromToken(FirebaseToken decodedToken) {
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();

        // 1. Chercher par UID Firebase (cas le plus courant pour les connexions répétées)
        Optional<User> existingUserByUid = userRepository.findByUid(uid);
        if (existingUserByUid.isPresent()) {
            return existingUserByUid.get();
        }

        // 2. Si non trouvé par UID, chercher par e-mail (cas de la première connexion après inscription, ou liaison)
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            User userToLink = existingUserByEmail.get();
            // Si l'utilisateur existe déjà (inscription classique) mais n'a pas de UID, on le lie.
            if (userToLink.getUid() == null || userToLink.getUid().isBlank()) {
                logger.info("Linking Firebase UID {} to existing local account {}", uid, email);
                userToLink.setUid(uid);
                if (!userToLink.isEmailVerified()) {
                    userToLink.setEmailVerified(true); // La connexion via Firebase confirme l'email
                }
                return userRepository.save(userToLink);
            }
            // Si l'UID correspond déjà, on retourne l'utilisateur.
            // Ce cas est normalement déjà géré par la première recherche, mais c'est une sécurité.
            return userToLink;
        }

        // 3. Si l'utilisateur n'existe VRAIMENT PAS, on le crée (cas d'une inscription via Google)
        logger.info("Creating new user from social login (Google) for UID: {}", uid);
        User newUser = new User();
        newUser.setUid(uid);
        newUser.setEmail(email);

        String name = decodedToken.getName();
        if (name != null && !name.isEmpty()) {
            String[] names = name.split(" ", 2);
            newUser.setFirstName(names[0]);
            if (names.length > 1) {
                newUser.setLastName(names[1]);
            }
        } else {
            newUser.setFirstName(email != null ? email.split("@")[0] : "User");
        }

        // On assigne un mot de passe aléatoire FORT et non devinable, car il ne sera jamais utilisé pour se connecter.
        // La connexion se fera toujours via Google.
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + UUID.randomUUID().toString()));
        newUser.setRole(UserRole.USER);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setEmailVerified(true);

        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByToken(FirebaseToken decodedToken) {
        return userRepository.findByUid(decodedToken.getUid());
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findUserByUid(String uid) {
        return userRepository.findByUid(uid);
    }

    @Transactional
    public UserProfileDto updateUserProfile(Long userId, UserProfileDto userProfileDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(userProfileDto.getFirstName());
        user.setLastName(userProfileDto.getLastName());
        user.setPreferredLanguage(userProfileDto.getPreferredLanguage());
        user.setPhoneNumber(userProfileDto.getPhoneNumber());
        user.setAddress(userProfileDto.getAddress());

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    @Transactional
    public UserProfileDto updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            user.setRole(UserRole.valueOf(roleName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + roleName);
        }

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    @Transactional
    public UserProfileDto updateUserStatus(Long userId, String statusName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            user.setStatus(UserStatus.valueOf(statusName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + statusName);
        }

        User updatedUser = userRepository.save(user);
        return convertToDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User updateFcmToken(String uid, String fcmToken) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFcmToken(fcmToken);
        return userRepository.save(user);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @SuppressWarnings("SpellCheckingInspection")
    public UserProfileDto convertToDto(User user) {
        String profileImageUrl = user.getProfileImageUrl();

        // ==================== CORRECTION : Transformer le chemin relatif en URL absolue ====================
        if (profileImageUrl != null && profileImageUrl.startsWith("/uploads/")) {
            profileImageUrl = backendBaseUrl + profileImageUrl;
        }
        // ==============================================================================================

        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getPreferredLanguage(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.isEmailVerified(),
                user.isMfaEnabled(),
                profileImageUrl // Utiliser la variable modifiée
        );
    }

    public AdminUserDto convertToAdminDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getUid(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getCreatedAt(),       // champ ajouté
                user.isEmailVerified(),  // champ ajouté
                user.isMfaEnabled()
        );
    }
    @Transactional
    public UserProfileDto updateUserProfileImage(String userEmail, MultipartFile file) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));

        // On utilise une nouvelle méthode dans FileStorageService pour plus de clarté
        String filePath = fileStorageService.storeProfileImage(file);

        user.setProfileImageUrl(filePath);
        User updatedUser = userRepository.save(user);

        return convertToDto(updatedUser);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    @Transactional
    public void createPasswordResetTokenForUser(String userEmail, HttpServletRequest request) { // <-- Ajout de HttpServletRequest
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // AUDIT LOG: Demande de réinitialisation du mot de passe
            auditLogService.logEvent(user, SecurityActionType.PASSWORD_RESET_REQUEST, request, "Demande de réinitialisation de mot de passe initiée.");

            try {
                // --- ÉTAPE 1 : CHERCHER ET SUPPRIMER L'ANCIEN JETON (si existant) ---
                Optional<PasswordResetToken> existingToken = tokenRepository.findByUser(user);
                if (existingToken.isPresent()) {
                    logger.info("Deleting existing password reset token for user: {}", user.getEmail());
                    tokenRepository.delete(existingToken.get());
                    // Forcer le flush pour s'assurer que la suppression est commitée
                    tokenRepository.flush();
                }

                // --- ÉTAPE 2 : CRÉER ET SAUVEGARDER LE NOUVEAU JETON ---
                String tokenValue = UUID.randomUUID().toString();
                PasswordResetToken myToken = new PasswordResetToken(tokenValue, user);
                tokenRepository.save(myToken);

                // --- ÉTAPE 3 : ENVOYER L'E-MAIL AVEC LE NOUVEAU JETON ---
                String resetUrl = frontendBaseUrl + "/reset-password?token=" + tokenValue;
                emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetUrl);

                logger.info("Password reset token created and email sent for user: {}", user.getEmail());

            } catch (DataIntegrityViolationException e) {
                logger.error("Data integrity violation when creating password reset token for user {}: {}", user.getEmail(), e.getMessage());
                // En cas d'erreur de contrainte, on peut tenter une dernière fois de nettoyer et recréer
                try {
                    Optional<PasswordResetToken> retryToken = tokenRepository.findByUser(user);
                    retryToken.ifPresent(token -> {
                        logger.info("Retrying: Deleting conflicting password reset token for user: {}", user.getEmail());
                        tokenRepository.delete(token);
                        tokenRepository.flush();
                    });

                    String tokenValue = UUID.randomUUID().toString();
                    PasswordResetToken myToken = new PasswordResetToken(tokenValue, user);
                    tokenRepository.save(myToken);

                    String resetUrl = frontendBaseUrl + "/reset-password?token=" + tokenValue;
                    emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetUrl);

                    logger.info("Password reset token created successfully on retry for user: {}", user.getEmail());
                } catch (Exception retryException) {
                    logger.error("Failed to create password reset token even on retry for user {}: {}", user.getEmail(), retryException.getMessage());
                    throw new RuntimeException("Unable to process password reset request. Please try again later.");
                }
            } catch (Exception e) {
                logger.error("Unexpected error creating password reset token for user {}: {}", user.getEmail(), e.getMessage(), e);
                throw new RuntimeException("An error occurred while processing your password reset request. Please try again later.");
            }
        } else {
            // Pour des raisons de sécurité, nous ne révélons pas si l'email existe ou non
            logger.info("Password reset requested for non-existent email: {}", userEmail);
        }
    }
    public Optional<String> validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passToken = tokenRepository.findByToken(token);
        if (passToken.isEmpty()) {
            return Optional.of("Invalid token");
        }
        if (passToken.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            return Optional.of("Token has expired");
        }
        return Optional.empty(); // Pas d'erreur, token valide
    }

    @Transactional
    public void changeUserPassword(String token, String newPassword, HttpServletRequest request) {
        PasswordResetToken passToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid or expired password reset token."));

        User user = passToken.getUser();
        String encodedPassword = passwordEncoder.encode(newPassword);

        // --- Mettre à jour Firebase ---
        String firebaseUid = user.getUid();
        if (firebaseUid != null && !firebaseUid.isBlank()) {
            try {
                UserRecord.UpdateRequest firebaserequest = new UserRecord.UpdateRequest(firebaseUid).setPassword(newPassword);
                FirebaseAuth.getInstance().updateUser(firebaserequest);
            } catch (FirebaseAuthException e) {
                logger.error("Error updating password in Firebase for UID {}: {}", firebaseUid, e.getMessage());
                throw new RuntimeException("Failed to update password in authentication service. Please try again.", e);
            }
        }

        // --- Mettre à jour la BDD locale ---
        user.setPassword(encodedPassword);
        userRepository.save(user);

        // --- NOUVEAU: Enregistrer l'historique ---
        savePasswordHistory(user, encodedPassword, PasswordChangeMethod.BY_RESET_LINK, request);

        // --- Supprimer le token ---
        tokenRepository.delete(passToken);

        logger.info("Password reset process completed for user: {}", user.getEmail());
        auditLogService.logEvent(user, SecurityActionType.PASSWORD_RESET_SUCCESS, request, "Le mot de passe a été réinitialisé avec succès via le formulaire de mot de passe oublié.");
    }
    @Transactional
    public void savePasswordHistory(User user, String encodedPassword, PasswordChangeMethod method, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        PasswordHistory historyEntry = new PasswordHistory(user, encodedPassword, method, ipAddress);
        passwordHistoryRepository.save(historyEntry);
    }
//    @Transactional
//    public void changePasswordFromProfile(User user, String oldPassword, String newPassword, HttpServletRequest request) {
//        // === ÉTAPE DE DIAGNOSTIC TEMPORAIRE - À SUPPRIMER APRÈS RÉSOLUTION ===
//        logger.info("=== PASSWORD CHANGE DEBUG START ===");
//        logger.info("DEBUG: User email: {}", user.getEmail());
//        logger.info("DEBUG: User ID: {}", user.getId());
//        logger.info("DEBUG: User UID: {}", user.getUid());
//        logger.info("DEBUG: Received old password: '{}'", oldPassword);
//        logger.info("DEBUG: Stored password hash length: {}", user.getPassword() != null ? user.getPassword().length() : "null");
//        logger.info("DEBUG: Stored password hash starts with: {}", user.getPassword() != null ? user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "null");
//
//        // Test de vérification du mot de passe
//        boolean passwordMatches = passwordEncoder.matches(oldPassword, user.getPassword());
//        logger.info("DEBUG: Password encoder matches result: {}", passwordMatches);
//
//        // Tester quelques variantes courantes si le mot de passe ne correspond pas
//        if (!passwordMatches) {
//            String[] commonVariants = {
//                    oldPassword.trim(),
//                    oldPassword.toLowerCase(),
//                    oldPassword.toUpperCase(),
//                    "Moha@2000", // Au cas où il y aurait un problème d'encodage
//                    "moha@2000"
//            };
//
//            for (String variant : commonVariants) {
//                if (passwordEncoder.matches(variant, user.getPassword())) {
//                    logger.warn("DEBUG: Password matches with variant: '{}'", variant);
//                    break;
//                }
//            }
//        }
//        logger.info("=== PASSWORD CHANGE DEBUG END ===");
//
//        // === LOGIQUE NORMALE (INCHANGÉE) ===
//
//        // 1. Vérifier si l'ancien mot de passe est correct
//        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
//            auditLogService.logEvent(user, SecurityActionType.PASSWORD_CHANGE_FAILED, request, "Ancien mot de passe incorrect.");
//            throw new IllegalArgumentException("Incorrect old password.");
//        }
//
//        // 2. Vérifier que le nouveau mot de passe n'est pas le même que l'ancien
//        if (passwordEncoder.matches(newPassword, user.getPassword())) {
//            throw new IllegalArgumentException("New password cannot be the same as the old password.");
//        }
//
//        String encodedNewPassword = passwordEncoder.encode(newPassword);
//        String firebaseUid = user.getUid();
//
//        // 3. Mettre à jour la BDD locale D'ABORD
//        user.setPassword(encodedNewPassword);
//        userRepository.save(user);
//
//        // 4. Enregistrer l'historique et l'audit du succès LOCAL
//        savePasswordHistory(user, encodedNewPassword, PasswordChangeMethod.BY_USER, request);
//        auditLogService.logEvent(user, SecurityActionType.PASSWORD_CHANGE_SUCCESS, request, "Mot de passe changé avec succès dans la base de données locale.");
//
//        // 5. Ensuite, tenter de synchroniser avec Firebase
//        if (firebaseUid != null && !firebaseUid.isBlank()) {
//            try {
//                logger.info("Attempting to sync password update in Firebase for UID: {}", firebaseUid);
//                UserRecord.UpdateRequest firebaserequest = new UserRecord.UpdateRequest(firebaseUid).setPassword(newPassword);
//                FirebaseAuth.getInstance().updateUser(firebaserequest);
//                logger.info("Successfully synced password in Firebase for UID: {}", firebaseUid);
//            } catch (FirebaseAuthException e) {
//                // NE PAS FAIRE ÉCHOUER LA TRANSACTION. On enregistre une erreur grave.
//                logger.error("CRITICAL: Failed to sync password in Firebase for UID {} after local DB update from profile. Manual intervention may be required. Error: {}", firebaseUid, e.getMessage());
//                // On pourrait ici envoyer un email à un admin pour l'alerter du problème de synchronisation.
//            }
//        }
//    }
//    @Transactional
//    public void adminForcePasswordSync(Long userId, String newPassword, HttpServletRequest request) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
//
//        String encodedPassword = passwordEncoder.encode(newPassword);
//        String firebaseUid = user.getUid();
//
//        // 1. Mettre à jour Firebase d'abord, car c'est l'opération la plus susceptible d'échouer.
//        if (firebaseUid != null && !firebaseUid.isBlank()) {
//            try {
//                logger.info("ADMIN ACTION: Forcing password update in Firebase for UID: {}", firebaseUid);
//                UserRecord.UpdateRequest firebaserequest = new UserRecord.UpdateRequest(firebaseUid).setPassword(newPassword);
//                FirebaseAuth.getInstance().updateUser(firebaserequest);
//                logger.info("ADMIN ACTION: Successfully updated password in Firebase for UID: {}", firebaseUid);
//            } catch (FirebaseAuthException e) {
//                logger.error("ADMIN ACTION: Error updating password in Firebase for UID {}: {}", firebaseUid, e.getMessage());
//                throw new RuntimeException("Failed to update password in authentication service. Please check Firebase logs.", e);
//            }
//        } else {
//            logger.warn("ADMIN ACTION: User with email {} has no Firebase UID. Password will only be updated in local DB.", user.getEmail());
//        }
//
//        // 2. Si Firebase réussit, mettre à jour la BDD locale.
//        logger.info("ADMIN ACTION: Updating password in local database for user: {}", user.getEmail());
//        user.setPassword(encodedPassword);
//        userRepository.save(user);
//
//        // 3. Enregistrer l'historique et l'audit
//        savePasswordHistory(user, encodedPassword, PasswordChangeMethod.BY_ADMIN, request); // Il faudra ajouter BY_ADMIN à l'enum
//        auditLogService.logEvent(user, SecurityActionType.PASSWORD_RESET_FORCED, request, "Password was forcibly reset by an administrator."); // Il faudra ajouter ce type d'action
//    }
    @Transactional(readOnly = true)
    public List<PasswordHistoryDto> getPasswordHistoryForUser(User user) {
        return passwordHistoryRepository.findTop5ByUserOrderByChangeDateDesc(user)
                .stream()
                .map(this::convertToPasswordHistoryDto)
                .collect(Collectors.toList());
    }
    private PasswordHistoryDto convertToPasswordHistoryDto(PasswordHistory history) {
        return new PasswordHistoryDto(
                history.getChangeDate(),
                history.getChangeMethod().name(),
                history.getIpAddress()
        );
    }
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "N/A";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
    @Transactional(readOnly = true)
    public Page<AdminUserDto> findAllUsersPaginated(String search, Pageable pageable) {
        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            // Si un terme de recherche est fourni, on utilise la nouvelle méthode du repository
            userPage = userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable
            );
        } else {
            // Sinon, on récupère simplement tous les utilisateurs de la page demandée
            userPage = userRepository.findAll(pageable);
        }
        // On convertit la Page<User> en Page<AdminUserDto>
        return userPage.map(this::convertToAdminDto);
    }
}