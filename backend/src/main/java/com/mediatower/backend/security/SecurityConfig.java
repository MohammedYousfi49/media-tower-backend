package com.mediatower.backend.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value; // <-- AJOUTER CET IMPORT

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    public SecurityConfig(FirebaseTokenFilter firebaseTokenFilter) {
        this.firebaseTokenFilter = firebaseTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                // --- DÉBUT DES MODIFICATIONS SUR LES EN-TÊTES ---
                .headers(headers -> headers
                        // Gardé pour la compatibilité avec la console H2
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)

                        // MODIFICATION : On retire .disable() pour activer l'en-tête X-Content-Type-Options=nosniff
                        // C'est une bonne pratique de sécurité.
                        // La ligne originale était : .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)

                        // AJOUT : En-tête Strict-Transport-Security (HSTS)
                        // Force les navigateurs à utiliser HTTPS pour une durée spécifiée (ici, 1 an)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )

                        // AJOUT : En-tête Content-Security-Policy (CSP)
                        // Protection majeure contre les attaques XSS.
                        // Cette politique de base est très restrictive et idéale pour une API.
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'; script-src 'self';")
                        )
                )
                // --- FIN DES MODIFICATIONS SUR LES EN-TÊTES ---
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // --- 1. Endpoints Publics (Logique existante conservée) ---
                        .requestMatchers(HttpMethod.POST, "/api/auth/**", "/api/promotions/validate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify-email/**", "/api/download/**", "/api/products/**", "/api/categories/**", "/api/tags/**", "/api/services/**", "/api/reviews/**", "/api/service-reviews/**", "/api/settings", "/api/stats/support/**", "/api/packs/**").permitAll()
                        .requestMatchers("/api/webhooks/**", "/ws/**").permitAll()
                        .requestMatchers(PathRequest.toH2Console()).permitAll()

                        // --- 2. Endpoints ADMIN (Logique existante conservée) ---
                        .requestMatchers("/api/promotions/**", "/api/admin/**", "/api/stats/**", "/api/files/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products", "/api/services", "/api/packs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/services/**", "/api/packs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/services/**", "/api/packs/**").hasRole("ADMIN")

                        // --- 3. Routes pour utilisateurs connectés (Logique existante conservée) ---
                        .requestMatchers(HttpMethod.GET, "/api/user/products", "/api/products/*/download-link").authenticated()

                        // --- 4. TOUT LE RESTE (Logique existante conservée) ---
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // NOTE: Pour la production, il est recommandé de charger ces URL depuis votre fichier application.properties
        configuration.setAllowedOrigins(List.of("http://localhost:5174", "http://localhost:5173", "http://localhost:5175", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}