package com.mediatower.backend.controller;

import com.mediatower.backend.dto.ServiceReviewDto;
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

    private final ServiceReviewService reviewService;

    public ServiceReviewController(ServiceReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // Public: tout le monde peut lire les avis d'un service
    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ServiceReviewDto>> getReviewsForService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(reviewService.getReviewsForService(serviceId));
    }

    // Client authentifié seulement: créer un avis
    @PostMapping("/service/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long serviceId,
            @RequestBody ServiceReviewDto dto) {
        try {
            ServiceReviewDto createdReview = reviewService.createServiceReview(firebaseUser.getUid(), serviceId, dto);
            return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // Gérer le cas où l'utilisateur n'a pas le droit de laisser un avis
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}