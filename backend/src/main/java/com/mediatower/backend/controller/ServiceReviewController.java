// Fichier : src/main/java/com/mediatower/backend/controller/ServiceReviewController.java (COMPLET ET CORRIGÉ)

package com.mediatower.backend.controller;

import com.mediatower.backend.dto.ServiceReviewDto;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.UserRepository;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.ServiceReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-reviews")
public class ServiceReviewController {

    // On n'a besoin que de ces deux dépendances
    private final ServiceReviewService serviceReviewService;
    private final UserRepository userRepository;

    // ▼▼▼ LE CONSTRUCTEUR EST CORRIGÉ ▼▼▼
    public ServiceReviewController(ServiceReviewService serviceReviewService, UserRepository userRepository) {
        this.serviceReviewService = serviceReviewService;
        this.userRepository = userRepository;
    }

    // Public: tout le monde peut lire les avis d'un service
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ServiceReviewDto>> getReviewsForService(@PathVariable Long serviceId) {
        // ▼▼▼ CORRECTION : On utilise la bonne variable 'serviceReviewService' ▼▼▼
        return ResponseEntity.ok(serviceReviewService.getReviewsForService(serviceId));
    }

    // Client authentifié seulement: créer un avis
    @PostMapping("/service/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long serviceId,
            @RequestBody ServiceReviewDto dto) {
        try {
            // ▼▼▼ CORRECTION : On utilise la bonne variable 'serviceReviewService' ▼▼▼
            ServiceReviewDto createdReview = serviceReviewService.createServiceReview(firebaseUser.getUid(), serviceId, dto);
            return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteServiceReview(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long id) {
        try {
            // Le reste du code était déjà correct et utilisait 'serviceReviewService'
            ServiceReviewDto existingReview = serviceReviewService.getReviewById(id)
                    .orElseThrow(() -> new RuntimeException("Service review not found with ID: " + id));

            boolean isAdmin = firebaseUser.getRole().name().equals("ADMIN");

            User reviewOwner = userRepository.findById(existingReview.getUserId())
                    .orElseThrow(() -> new RuntimeException("Review owner not found"));

            if (!isAdmin && !reviewOwner.getUid().equals(firebaseUser.getUid())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            serviceReviewService.deleteReview(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}