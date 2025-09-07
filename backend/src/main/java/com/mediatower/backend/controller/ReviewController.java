package com.mediatower.backend.controller;

import com.mediatower.backend.dto.ReviewDto;
import com.mediatower.backend.model.User;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.repository.UserRepository;

import com.mediatower.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final UserRepository userRepository;


    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ReviewDto> getAllReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/product/{productId}")
    public List<ReviewDto> getReviewsByProductId(@PathVariable Long productId) {
        return reviewService.getReviewsByProductId(productId);
    }

    @PostMapping("/product/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewDto reviewDto) {
        try {
            // --- CORRECTION ICI ---
            ReviewDto createdReview = reviewService.createReview(firebaseUser.getUid(), productId, reviewDto);
            return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // La vérification de la propriété se fait dans la méthode
    public ResponseEntity<?> updateReview(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long id,
            @Valid @RequestBody ReviewDto reviewDto) {
        try {
            ReviewDto existingReview = reviewService.getReviewById(id)
                    .orElseThrow(() -> new RuntimeException("Review not found with ID: " + id));

            // --- CORRECTION ICI (et simplification) ---
            if (!firebaseUser.getRole().name().equals("ADMIN") && !existingReview.getUserId().toString().equals(firebaseUser.getUid())) {
                return new ResponseEntity<>("You are not authorized to update this review.", HttpStatus.FORBIDDEN);
            }

            ReviewDto updatedReview = reviewService.updateReview(id, reviewDto);
            return ResponseEntity.ok(updatedReview);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long id) {
        try {
            ReviewDto existingReview = reviewService.getReviewById(id)
                    .orElseThrow(() -> new RuntimeException("Review not found with ID: " + id));

            boolean isAdmin = firebaseUser.getRole().name().equals("ADMIN");

            // Le code utilise déjà userRepository, il fallait juste l'injecter.
            User reviewOwner = userRepository.findById(existingReview.getUserId())
                    .orElseThrow(() -> new RuntimeException("Review owner not found"));

            if (!isAdmin && !reviewOwner.getUid().equals(firebaseUser.getUid())) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            reviewService.deleteReview(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}