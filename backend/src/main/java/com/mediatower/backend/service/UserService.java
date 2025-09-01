package com.mediatower.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.PasswordHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;

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

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetTokenRepository tokenRepository;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;


    public UserService(UserRepository userRepository, PasswordHistoryRepository passwordHistoryRepository, PasswordEncoder passwordEncoder, EmailService emailService, PasswordResetTokenRepository tokenRepository, AuditLogService auditLogService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenRepository = tokenRepository;
        this.auditLogService = auditLogService;
        this.fileStorageService = fileStorageService;
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

        // 1. Chercher par UID Firebase (cas le plus courant)
        Optional<User> existingUserByUid = userRepository.findByUid(uid);
        if (existingUserByUid.isPresent()) {
            logger.debug("User found by UID: {}", uid);
            return existingUserByUid.get();
        }

        // 2. Si non trouvé par UID, chercher par e-mail (cas du conflit/liaison)
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            User userToLink = existingUserByEmail.get();
            // L'utilisateur existe mais n'a pas de UID, on le lie.
            if (userToLink.getUid() == null || userToLink.getUid().isBlank()) {
                logger.info("Linking Firebase UID {} to existing local account {}", uid, email);
                userToLink.setUid(uid);
                // La connexion Google confirme que l'e-mail est vérifié
                if (!userToLink.isEmailVerified()) {
                    userToLink.setEmailVerified(true);
                }
                return userRepository.save(userToLink);
            } else {
                // Cas très rare : l'e-mail est déjà lié à un AUTRE UID. C'est une erreur.
                logger.error("Email {} is already associated with a different Firebase UID: {} (attempted: {})", email, userToLink.getUid(), uid);
                throw new IllegalStateException("This email is already linked to a different social account.");
            }
        }

        // 3. Si l'utilisateur n'existe pas du tout, on le crée.
        logger.info("Creating new user from Firebase token for UID: {}", uid);
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
            newUser.setLastName("");
        }

        // On lui donne un mot de passe aléatoire car il est géré par Google
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setRole(UserRole.USER);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setEmailVerified(true); // L'e-mail est vérifié par Google

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
                user.getProfileImageUrl()
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
                user.getAddress()
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
    @Transactional
    public void changePasswordFromProfile(User user, String oldPassword, String newPassword, HttpServletRequest request) {
        // 1. Vérifier si l'ancien mot de passe est correct
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect old password.");
        }

        // 2. (Optionnel mais recommandé) Vérifier que le nouveau mot de passe n'est pas le même que l'ancien
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as the old password.");
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);

        // 3. Mettre à jour Firebase
        String firebaseUid = user.getUid();
        if (firebaseUid != null && !firebaseUid.isBlank()) {
            try {
                UserRecord.UpdateRequest firebaserequest = new UserRecord.UpdateRequest(firebaseUid).setPassword(newPassword);
                FirebaseAuth.getInstance().updateUser(firebaserequest);
            } catch (FirebaseAuthException e) {
                throw new RuntimeException("Failed to update password in authentication service.", e);
            }
        }

        // 4. Mettre à jour la BDD locale
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        // 5. Enregistrer l'historique et l'audit
        savePasswordHistory(user, encodedNewPassword, PasswordChangeMethod.BY_USER, request);
        auditLogService.logEvent(user, SecurityActionType.PASSWORD_CHANGE_SUCCESS, request, "Mot de passe changé depuis le profil.");
    }
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
}