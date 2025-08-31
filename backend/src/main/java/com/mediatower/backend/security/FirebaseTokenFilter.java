package com.mediatower.backend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.mediatower.backend.model.User;
import com.mediatower.backend.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private final UserService userService;
    private final RequestMatcher publicPathsMatcher;

    public FirebaseTokenFilter(UserService userService) {
        this.userService = userService;
        List<RequestMatcher> matchers = Arrays.asList(
                // Endpoints d'authentification publics SEULEMENT
                new AntPathRequestMatcher("/api/auth/register", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/api/auth/verify-email/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/auth/resend-verification", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/api/auth/forgot-password", "POST"),
                new AntPathRequestMatcher("/api/auth/reset-password", "POST"),

                // IMPORTANT: /api/auth/login et /api/auth/verify-2fa NE SONT PLUS dans cette liste
                // car ils doivent maintenant passer par le filtre d'authentification

                // Autres endpoints publics
                new AntPathRequestMatcher("/api/webhooks/**"),
                new AntPathRequestMatcher("/api/download/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/service-reviews/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/products/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/categories/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/tags/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/services/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/reviews/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/settings", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/ws/**"),
                new AntPathRequestMatcher("/api/chats/offline-message", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/h2-console/**")
        );
        publicPathsMatcher = new OrRequestMatcher(matchers);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        boolean matches = publicPathsMatcher.matches(request);
        String requestInfo = request.getMethod() + " " + request.getRequestURI();

        logger.debug("Checking filter bypass for: {}, Public path: {}", requestInfo, matches);

        // Log spécialement pour les endpoints d'auth pour debugging
        if (request.getRequestURI().startsWith("/api/auth/") || request.getRequestURI().startsWith("/api/mfa/")) {
            logger.info("Auth/MFA endpoint check: {}, Bypass filter: {}", requestInfo, matches);
        }

        return matches;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String requestInfo = method + " " + requestURI;

        logger.debug("Processing authenticated request: {}", requestInfo);

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            try {
                logger.debug("Validating Firebase token for request: {}", requestInfo);
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                logger.debug("Valid Firebase token for UID: {} on request: {}", decodedToken.getUid(), requestInfo);

                User userInDb = userService.findOrCreateUserFromToken(decodedToken);
                logger.debug("User authenticated: {} with role: {} for request: {}",
                        userInDb.getEmail(), userInDb.getRole().name(), requestInfo);

                FirebaseUser firebaseUser = FirebaseUser.create(userInDb);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        firebaseUser, null, firebaseUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("Authentication successfully set for: {} on request: {}",
                        firebaseUser.getUsername(), requestInfo);

            } catch (Exception e) {
                logger.warn("Firebase token validation failed for request {}: {}", requestInfo, e.getMessage());
                SecurityContextHolder.clearContext();

                response.setContentType("application/json; charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid or expired authentication token: " + e.getMessage() + "\"}");
                return;
            }
        } else {
            logger.warn("Missing Authorization header for protected endpoint: {}", requestInfo);
            SecurityContextHolder.clearContext();

            response.setContentType("application/json; charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Authorization header missing. Please provide a valid Bearer token.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}