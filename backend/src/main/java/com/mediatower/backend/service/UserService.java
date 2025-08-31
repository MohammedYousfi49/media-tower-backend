package com.mediatower.backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.beans.factory.annotation.Value;

import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.mediatower.backend.dto.AdminUserDto;
import com.mediatower.backend.dto.RegisterRequest;
import com.mediatower.backend.dto.UserProfileDto;
import com.mediatower.backend.model.PasswordResetToken;
import com.mediatower.backend.model.User;
import com.mediatower.backend.model.UserRole;
import com.mediatower.backend.model.UserStatus;
import com.mediatower.backend.repository.PasswordResetTokenRepository;
import com.mediatower.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetTokenRepository tokenRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, PasswordResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.tokenRepository = tokenRepository;
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

        logger.debug("Finding or creating user for Firebase UID: {} with email: {}", uid, email);

        Optional<User> existingUserByUid = userRepository.findByUid(uid);
        if (existingUserByUid.isPresent()) {
            logger.debug("User found by UID: {}", uid);
            return existingUserByUid.get();
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            User user = existingUserByEmail.get();
            if (user.getUid() == null || user.getUid().isBlank()) {
                logger.info("Linking Firebase UID {} to existing user {}", uid, email);
                user.setUid(uid);
                user.setEmailVerified(true);
                return userRepository.save(user);
            } else if (!user.getUid().equals(uid)) {
                logger.error("Email {} is already associated with a different Firebase UID: {} vs {}", email, user.getUid(), uid);
                throw new IllegalStateException("Email is already associated with another Firebase account.");
            }
            return user;
        }

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
            String defaultFirstName = email != null ? email.split("@")[0] : "User";
            newUser.setFirstName(defaultFirstName);
            newUser.setLastName("");
        }

        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setRole(UserRole.USER); // Assurez-vous que le rôle USER est toujours attribué ici aussi
        newUser.setStatus(UserStatus.ACTIVE); // Assurez-vous que le statut ACTIVE est toujours attribué
        newUser.setEmailVerified(true);

        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            logger.warn("Data integrity violation when creating user with email {}, retrying lookup: {}", email, e.getMessage());
            return userRepository.findByUid(uid)
                    .orElseGet(() -> userRepository.findByEmail(email)
                            .orElseThrow(() -> new IllegalStateException("Failed to create user, and cannot find existing user after retry.")));
        }
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
                user.isMfaEnabled()
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

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    @Transactional
    public void createPasswordResetTokenForUser(String userEmail) {
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

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

    @Transactional // Il est bon de rendre cette opération transactionnelle
    public void changeUserPassword(String token, String newPassword) {
        PasswordResetToken passToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid or expired password reset token."));

        User user = passToken.getUser();
        String firebaseUid = user.getUid();

        // --- ÉTAPE 1 : Mettre à jour le mot de passe dans Firebase D'ABORD ---
        if (firebaseUid != null && !firebaseUid.isBlank()) {
            try {
                logger.info("Attempting to update password in Firebase for UID: {}", firebaseUid);
                UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(firebaseUid)
                        .setPassword(newPassword);
                FirebaseAuth.getInstance().updateUser(request);
                logger.info("Successfully updated password in Firebase for UID: {}", firebaseUid);
            } catch (FirebaseAuthException e) {
                logger.error("Error updating password in Firebase for UID {}: {}", firebaseUid, e.getMessage());
                // Si la mise à jour Firebase échoue, on ne continue pas.
                throw new RuntimeException("Failed to update password in authentication service. Please try again.", e);
            }
        } else {
            // Cas où l'utilisateur n'est pas lié à Firebase (par exemple, inscription classique non terminée)
            logger.warn("User with email {} has no Firebase UID. Updating password only in local DB.", user.getEmail());
        }

        // --- ÉTAPE 2 : Si Firebase est à jour, mettre à jour la base de données locale ---
        logger.info("Updating password in local database for user: {}", user.getEmail());
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // --- ÉTAPE 3 : Supprimer le token après utilisation ---
        tokenRepository.delete(passToken);
        logger.info("Password reset process completed for user: {}", user.getEmail());
    }
}