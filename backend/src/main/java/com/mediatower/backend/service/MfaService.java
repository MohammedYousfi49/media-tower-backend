package com.mediatower.backend.service;

import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MfaService {

    private static final Logger logger = LoggerFactory.getLogger(MfaService.class);
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public MfaService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateNewSecret() {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }

    public String getQrCodeDataUri(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("MediaTower")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        QrGenerator generator = new ZxingPngQrGenerator();
        try {
            byte[] imageData = generator.generate(data);
            return Utils.getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code", e);
        }
    }

    public boolean isOtpValid(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String sanitizedCode = code.trim().replaceAll("[^0-9]", "");
        logger.debug("Validating TOTP code: {} for secret: {}", sanitizedCode, secret);

        // Configurer un CodeVerifier avec une tolérance de ±1 période
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, sanitizedCode); // Tolérance intégrée via TimeProvider
    }

    private String generateSingleRecoveryCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                code.append("-");
            }
            code.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return code.toString();
    }

    public List<String> generateRecoveryCodes(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(generateSingleRecoveryCode());
        }
        return codes;
    }

    public MfaVerificationResult verifyMfaCode(String email, String code) {
        logger.debug("Verifying MFA code: {} for user: {}", code, email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new MfaVerificationResult(false, MfaVerificationResult.Type.INVALID_USER);
        }

        User user = userOpt.get();

        if (!user.isMfaEnabled()) {
            return new MfaVerificationResult(false, MfaVerificationResult.Type.MFA_NOT_ENABLED);
        }

        String trimmedCode = code.trim();

        if (user.getMfaSecret() != null && isOtpValid(user.getMfaSecret(), trimmedCode)) {
            logger.debug("TOTP validation succeeded for user: {}", email);
            return new MfaVerificationResult(true, MfaVerificationResult.Type.TOTP_SUCCESS, user.getAvailableRecoveryCodesCount());
        }

        if (user.useRecoveryCode(trimmedCode)) {
            userRepository.save(user);
            logger.debug("Recovery code used for user: {}, remaining: {}", email, user.getAvailableRecoveryCodesCount());
            return new MfaVerificationResult(true, MfaVerificationResult.Type.RECOVERY_CODE_USED, user.getAvailableRecoveryCodesCount());
        }

        logger.warn("Invalid MFA code for user: {}", email);
        return new MfaVerificationResult(false, MfaVerificationResult.Type.INVALID_CODE);
    }

    public List<String> regenerateRecoveryCodes(User user) {
        if (!user.isMfaEnabled()) {
            throw new IllegalStateException("MFA must be enabled to generate recovery codes");
        }
        List<String> newCodes = generateRecoveryCodes(10);
        user.setRecoveryCodes(newCodes);
        user.setRecoveryCodesGeneratedAt(LocalDateTime.now());
        userRepository.save(user);
        return newCodes;
    }

    public MfaActivationResult activateMfaForUser(User user) {
        String tempSecret = user.getTempMfaSecret();
        List<String> recoveryCodes = generateRecoveryCodes(10);
        user.setMfaSecret(tempSecret);
        user.setTempMfaSecret(null);
        user.setMfaEnabled(true);
        user.setRecoveryCodes(recoveryCodes);
        user.setRecoveryCodesGeneratedAt(LocalDateTime.now());
        userRepository.save(user);
        return new MfaActivationResult(true, recoveryCodes);
    }

    public void disableMfa(User user) {
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setTempMfaSecret(null);
        user.setRecoveryCodes(null);
        user.setRecoveryCodesGeneratedAt(null);
        userRepository.save(user);
    }

    public static class MfaVerificationResult {
        private final boolean valid;
        private final Type type;
        private final int remainingRecoveryCodes;

        public enum Type { TOTP_SUCCESS, RECOVERY_CODE_USED, INVALID_CODE, INVALID_USER, MFA_NOT_ENABLED }

        public MfaVerificationResult(boolean valid, Type type) {
            this(valid, type, -1);
        }

        public MfaVerificationResult(boolean valid, Type type, int remainingRecoveryCodes) {
            this.valid = valid;
            this.type = type;
            this.remainingRecoveryCodes = remainingRecoveryCodes;
        }

        public boolean isValid() { return valid; }
        public Type getType() { return type; }
        public boolean isRecoveryCodeUsed() { return type == Type.RECOVERY_CODE_USED; }
        public int getRemainingRecoveryCodes() { return remainingRecoveryCodes; }
    }

    public static class MfaActivationResult {
        private final boolean success;
        private final List<String> recoveryCodes;
        public MfaActivationResult(boolean success, List<String> recoveryCodes) { this.success = success; this.recoveryCodes = recoveryCodes; }
        public boolean isSuccess() { return success; }
        public List<String> getRecoveryCodes() { return recoveryCodes; }
    }
}