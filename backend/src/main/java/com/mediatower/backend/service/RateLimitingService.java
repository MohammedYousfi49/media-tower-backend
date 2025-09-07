package com.mediatower.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mediatower.backend.config.CacheConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import io.github.bucket4j.ConsumptionProbe;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    // Cache pour les tentatives de renvoi d'email de vérification
    private final Cache<String, RateLimitAttempt> resendVerificationCache;
    private final Map<String, Bucket> loginAttemptCache = new ConcurrentHashMap<>();

    // Cache pour les tentatives de forgot password par email
    private final Cache<String, RateLimitAttempt> forgotPasswordCache;

    // Cache pour les requêtes par IP (utilise bucket4j)
    private final Map<String, Bucket> ipRequestCache = new ConcurrentHashMap<>();

    public RateLimitingService(CacheManager cacheManager) {
        // Cache pour resend verification (existant)
        this.resendVerificationCache = (Cache<String, RateLimitAttempt>) cacheManager.getCache(CacheConfig.RESEND_VERIFICATION_CACHE).getNativeCache();

        // Nouveau cache pour forgot password
        this.forgotPasswordCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES) // 15 minutes d'expiration
                .maximumSize(10_000)
                .build();
    }

    /**
     * Rate limiting pour resend verification email
     */
    public boolean allowResend(String email, int maxAttempts, int timeWindowMinutes) {
        RateLimitAttempt attempt = resendVerificationCache.get(email, k -> new RateLimitAttempt(1, LocalDateTime.now()));

        if (attempt == null) {
            resendVerificationCache.put(email, new RateLimitAttempt(1, LocalDateTime.now()));
            return true;
        }

        // Si la fenêtre de temps est passée, réinitialiser le compteur
        if (attempt.firstAttemptTime.plusMinutes(timeWindowMinutes).isBefore(LocalDateTime.now())) {
            logger.info("Rate limit for resend verification email {} reset due to time window expiration.", email);
            resendVerificationCache.put(email, new RateLimitAttempt(1, LocalDateTime.now()));
            return true;
        }

        // Si le nombre de tentatives est inférieur à la limite, incrémenter et autoriser
        if (attempt.attempts < maxAttempts) {
            attempt.attempts++;
            resendVerificationCache.put(email, attempt);
            logger.debug("Email {} allowed to resend verification. Attempts: {}/{}", email, attempt.attempts, maxAttempts);
            return true;
        }

        logger.warn("Rate limit exceeded for resend verification email {}. Attempts: {}/{}.", email, attempt.attempts, maxAttempts);
        return false;
    }

    /**
     * Rate limiting pour forgot password par email avec retour du résultat détaillé
     */
    public RateLimitResult checkForgotPasswordLimit(String email, int maxAttempts, int timeWindowMinutes) {
        RateLimitAttempt attempt = forgotPasswordCache.get(email, k -> new RateLimitAttempt(0, LocalDateTime.now()));

        if (attempt == null) {
            attempt = new RateLimitAttempt(1, LocalDateTime.now());
            forgotPasswordCache.put(email, attempt);
            return new RateLimitResult(true, 0, maxAttempts);
        }

        // Calculer le temps de reset basé sur la DERNIÈRE tentative
        LocalDateTime resetTime = attempt.lastAttemptTime.plusMinutes(timeWindowMinutes);

        // Si la fenêtre de temps est passée, réinitialiser le compteur
        if (resetTime.isBefore(LocalDateTime.now())) {
            logger.info("Rate limit for forgot password email {} reset due to time window expiration.", email);
            attempt = new RateLimitAttempt(1, LocalDateTime.now());
            forgotPasswordCache.put(email, attempt);
            return new RateLimitResult(true, 0, maxAttempts);
        }

        // Si le nombre de tentatives est inférieur à la limite, incrémenter et autoriser
        if (attempt.attempts < maxAttempts) {
            attempt.attempts++;
            attempt.lastAttemptTime = LocalDateTime.now(); // Mettre à jour le temps de la dernière tentative
            forgotPasswordCache.put(email, attempt);
            logger.debug("Email {} allowed to request forgot password. Attempts: {}/{}", email, attempt.attempts, maxAttempts);
            return new RateLimitResult(true, 0, maxAttempts);
        }

        // Calculer le temps restant basé sur la dernière tentative
        long secondsRemaining = Duration.between(LocalDateTime.now(), resetTime).getSeconds();

        logger.warn("Rate limit exceeded for forgot password email {}. Attempts: {}/{}.", email, attempt.attempts, maxAttempts);
        return new RateLimitResult(false, secondsRemaining, maxAttempts);
    }

    /**
     * Méthode de compatibilité (garde l'ancienne signature)
     */
    public boolean allowForgotPassword(String email, int maxAttempts, int timeWindowMinutes) {
        return checkForgotPasswordLimit(email, maxAttempts, timeWindowMinutes).isAllowed();
    }

    /**
     * Rate limiting par IP (pour les requêtes générales)
     */
    public boolean allowRequestFromIp(String ip) {
        Bucket bucket = ipRequestCache.computeIfAbsent(ip, this::createNewIpBucket);
        return bucket.tryConsume(1);
    }

    private Bucket createNewIpBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket.builder().addLimit(limit).build();
    }

    // Classe interne pour stocker l'état du rate limit
    private static class RateLimitAttempt {
        int attempts;
        LocalDateTime firstAttemptTime;
        LocalDateTime lastAttemptTime; // Nouveau champ ajouté

        public RateLimitAttempt(int attempts, LocalDateTime attemptTime) {
            this.attempts = attempts;
            this.firstAttemptTime = attemptTime;
            this.lastAttemptTime = attemptTime; // Initialiser avec la même valeur
        }
    }

    // Classe pour retourner le résultat du rate limiting avec le temps restant
    public static class RateLimitResult {
        private final boolean allowed;
        private final long secondsRemaining;
        private final int maxAttempts;

        public RateLimitResult(boolean allowed, long secondsRemaining, int maxAttempts) {
            this.allowed = allowed;
            this.secondsRemaining = Math.max(0, secondsRemaining); // Pas de temps négatif
            this.maxAttempts = maxAttempts;
        }

        public boolean isAllowed() { return allowed; }
        public long getSecondsRemaining() { return secondsRemaining; }
        public int getMaxAttempts() { return maxAttempts; }
    }
    /**
     * Vérifie si une tentative de connexion est autorisée depuis une adresse IP.
     * Si la tentative est bloquée, retourne le temps d'attente en secondes.
     *
     * @param ip L'adresse IP du client.
     * @return Un objet RateLimitResult. `isAllowed()` sera `false` si la limite est dépassée.
     */
    public RateLimitResult checkLoginAttempt(String ip) {
        Bucket bucket = loginAttemptCache.computeIfAbsent(ip, key -> {
            // Configuration stricte : 10 tentatives autorisées, puis recharge 1 tentative toutes les 30 secondes.
            // Cela signifie qu'après 10 échecs, l'utilisateur doit attendre 30s pour un nouvel essai.
            Bandwidth limit = Bandwidth.classic(10, Refill.intervally(1, Duration.ofSeconds(30)));
            return Bucket.builder().addLimit(limit).build();
        });

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new RateLimitResult(true, 0, 10); // Autorisé
        } else {
            // Calcul du temps d'attente en secondes
            long secondsRemaining = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            logger.warn("Tentative de connexion bloquée pour l'IP {}. Attente de {} secondes.", ip, secondsRemaining);
            return new RateLimitResult(false, secondsRemaining, 10); // Bloqué
        }
    }
}