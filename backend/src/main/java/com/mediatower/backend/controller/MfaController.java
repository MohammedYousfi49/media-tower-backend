package com.mediatower.backend.controller;

import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.UserRepository;
import com.mediatower.backend.service.MfaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {
    private static final Logger logger = LoggerFactory.getLogger(MfaController.class);

    private final MfaService mfaService;
    private final UserRepository userRepository;

    public MfaController(MfaService mfaService, UserRepository userRepository) {
        this.mfaService = mfaService;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getMfaStatus(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ResponseEntity.ok(Map.of(
                "enabled", user.isMfaEnabled(),
                "hasRecoveryCodes", user.hasRecoveryCodes(),
                "recoveryCodesCount", user.getAvailableRecoveryCodesCount()
        ));
    }

    @GetMapping("/setup")
    public ResponseEntity<?> setupMfa(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.isMfaEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("message", "MFA is already enabled."));
        }
        String tempSecret = mfaService.generateNewSecret();
        user.setTempMfaSecret(tempSecret);
        userRepository.save(user);
        String qrCodeDataUri = mfaService.getQrCodeDataUri(tempSecret, user.getEmail());
        return ResponseEntity.ok(Map.of("secret", tempSecret, "qrCodeUri", qrCodeDataUri));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody Map<String, String> payload, Authentication authentication) {
        String code = payload.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Code is required."));
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String secretToValidate = user.getTempMfaSecret();
        if (secretToValidate == null || secretToValidate.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "MFA setup has not been initiated."));
        }

        if (!mfaService.isOtpValid(secretToValidate, code)) {
            logger.warn("Invalid temporary MFA code for user {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid MFA code. Please try again."));
        }

        MfaService.MfaActivationResult result = mfaService.activateMfaForUser(user);

        return ResponseEntity.ok(Map.of(
                "message", "MFA has been successfully enabled.",
                "enabled", true,
                "recoveryCodes", result.getRecoveryCodes()
        ));
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa(@RequestBody Map<String, String> payload, Authentication authentication) {
        String code = payload.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "MFA code is required."));
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        logger.info("Attempting MFA disable for user: {}", email);

        MfaService.MfaVerificationResult result = mfaService.verifyMfaCode(email, code);
        if (!result.isValid()) {
            logger.warn("Invalid MFA code attempt for user: {} - Result: {}", email, result.getType());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA code."));
        }

        try {
            mfaService.disableMfa(user);
            logger.info("MFA successfully disabled for user: {} with method: {}", email, result.getType());
            String message = "MFA has been disabled.";
            if (result.isRecoveryCodeUsed()) {
                message += " (Recovery code was used)";
            }
            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "enabled", false,
                    "remainingRecoveryCodes", result.getRemainingRecoveryCodes()
            ));
        } catch (Exception e) {
            logger.error("Error disabling MFA for user {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to disable MFA."));
        }
    }

    @PostMapping("/regenerate-recovery-codes")
    public ResponseEntity<?> regenerateRecoveryCodes(@RequestBody Map<String, String> payload, Authentication authentication) {
        String code = payload.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "MFA code is required."));
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        logger.info("Attempting recovery codes regeneration for user: {}", email);

        MfaService.MfaVerificationResult result = mfaService.verifyMfaCode(email, code);
        if (!result.isValid()) {
            logger.warn("Invalid MFA code attempt for regeneration for user: {} - Result: {}", email, result.getType());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA code."));
        }

        try {
            List<String> newCodes = mfaService.regenerateRecoveryCodes(user);
            logger.info("Recovery codes regenerated for user: {} with method: {}", email, result.getType());
            return ResponseEntity.ok(Map.of(
                    "message", "Recovery codes have been regenerated.",
                    "recoveryCodes", newCodes,
                    "remainingRecoveryCodes", user.getAvailableRecoveryCodesCount()
            ));
        } catch (Exception e) {
            logger.error("Error regenerating recovery codes for user {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to regenerate recovery codes."));
        }
    }

    @GetMapping("/recovery-codes/count")
    public ResponseEntity<?> getRecoveryCodesCount(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ResponseEntity.ok(Map.of(
                "count", user.getAvailableRecoveryCodesCount(),
                "hasRecoveryCodes", user.hasRecoveryCodes()
        ));
    }
}