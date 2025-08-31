package com.mediatower.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching // Active la gestion du cache dans Spring
public class CacheConfig {

    public static final String RESEND_VERIFICATION_CACHE = "resendVerificationAttempts";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES) // Le cache expire après 60 minutes
                .maximumSize(10_000)); // Taille maximale du cache pour éviter la consommation excessive de mémoire
        return cacheManager;
    }
}