package com.mediatower.backend.controller;

import com.mediatower.backend.dto.RegisterRequest;
import com.mediatower.backend.model.SecurityActionType;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.UserRepository;
import com.mediatower.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final RateLimitingService rateLimitingService;
    private final MfaService mfaService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;// <-- Dépendance ajoutée
    private final DeviceService deviceService;


    public AuthController(UserService userService, RateLimitingService rateLimitingService,
                          UserRepository userRepository, PasswordEncoder passwordEncoder,
                          MfaService mfaService, AuditLogService auditLogService, DeviceService deviceService) {
        this.userService = userService;
        this.rateLimitingService = rateLimitingService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mfaService = mfaService;
        this.auditLogService = auditLogService;
        this.deviceService = deviceService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(HttpServletRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        auditLogService.logEvent(user, SecurityActionType.LOGIN_SUCCESS, request, "Authentification initiale réussie.");

        if (user.getStatus() != com.mediatower.backend.model.UserStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is not active.", "accountInactive", true));
        }
        if (!user.isEmailVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Email not verified.", "emailNotVerified", true));
        }

        if (user.isMfaEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "status", "mfa_required",
                    "hasRecoveryCodes", user.hasRecoveryCodes(),
                    "recoveryCodesCount", user.getAvailableRecoveryCodesCount()
            ));
        } else {
            // Connexion complète ici, on vérifie l'appareil.
            deviceService.handleDeviceVerification(user, request);
            return ResponseEntity.ok(Map.of("status", "success", "user", userService.convertToDto(user)));
        }
    }

    // ▼▼▼ MÉTHODE MODIFIÉE ▼▼▼
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(HttpServletRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        String email = authentication.getName();
        // La méthode findOrCreate a déjà fait son travail de liaison de compte ici
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        auditLogService.logEvent(user, SecurityActionType.LOGIN_SUCCESS, request, "Authentification Google initiale réussie.");

        if (user.getStatus() != com.mediatower.backend.model.UserStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is not active.", "accountInactive", true));
        }

        if (user.isMfaEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "status", "mfa_required",
                    "hasRecoveryCodes", user.hasRecoveryCodes(),
                    "recoveryCodesCount", user.getAvailableRecoveryCodesCount()
            ));
        } else {
            // Connexion Google complète ici, on vérifie l'appareil.
            deviceService.handleDeviceVerification(user, request);
            return ResponseEntity.ok(Map.of("status", "success", "user", userService.convertToDto(user)));
        }
    }

    // ▼▼▼ MÉTHODE MODIFIÉE ▼▼▼
    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verifyTwoFactorAuth(@RequestBody Map<String, String> payload, HttpServletRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required."));
        }
        String code = payload.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "MFA code is required."));
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found during 2FA verification"));

        if (!user.isMfaEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "MFA is not enabled for this account."));
        }

        MfaService.MfaVerificationResult result = mfaService.verifyMfaCode(email, code);

        if (!result.isValid()) {
            auditLogService.logEvent(user, SecurityActionType.MFA_VERIFICATION_FAILED, request, "Code invalide fourni.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA code."));
        }

        auditLogService.logEvent(user, SecurityActionType.MFA_VERIFICATION_SUCCESS, request, "Méthode : " + result.getType().name());

        // Connexion 2FA complète ici, on vérifie l'appareil.
        deviceService.handleDeviceVerification(user, request);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "user", userService.convertToDto(user),
                "verificationMethod", result.getType().name(),
                "remainingRecoveryCodes", result.getRemainingRecoveryCodes() >= 0 ? result.getRemainingRecoveryCodes() : user.getAvailableRecoveryCodesCount(),
                "completed", result.isRecoveryCodeUsed()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        // La journalisation pour l'inscription sera gérée dans le UserService pour avoir l'objet User
        try {
            userService.processRegistration(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful. Please check your email to verify your account."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/verify-email/{token}")
    public ResponseEntity<?> verifyEmail(@PathVariable("token") String token) {
        // La journalisation sera gérée dans le UserService
        try {
            userService.verifyUser(token);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }


    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }
        if (!rateLimitingService.allowResend(email.trim().toLowerCase(), 5, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "Too many requests. Please try again later."));
        }
        try {
            userService.resendVerificationEmail(email.trim().toLowerCase());
            return ResponseEntity.ok(Map.of("message", "If your email is in our system, a new verification email has been sent."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        // La journalisation sera gérée dans le UserService
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }

        RateLimitingService.RateLimitResult rateLimitResult = rateLimitingService.checkForgotPasswordLimit(email, 5, 15);
        if (!rateLimitResult.isAllowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", "Too many password reset requests."));
        }

        try {
            userService.createPasswordResetTokenForUser(email, request); // <-- Passage de request
            return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
        } catch (Exception e) {
            logger.error("Error in forgotPassword for {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred."));
        }
    }


    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> requestBody, HttpServletRequest request) { // <-- Ajout de HttpServletRequest
        // La journalisation sera gérée dans le UserService
        String token = requestBody.get("token");
        String newPassword = requestBody.get("password");

        Optional<String> validationError = userService.validatePasswordResetToken(token);
        if (validationError.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError.get()));
        }

        userService.changeUserPassword(token, newPassword, request); // <-- Passage de request
        return ResponseEntity.ok(Map.of("message", "Your password has been reset successfully."));
    }
}