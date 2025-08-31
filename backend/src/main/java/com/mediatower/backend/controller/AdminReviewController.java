// Fichier : src/main/java/com/mediatower/backend/controller/AdminReviewController.java (NOUVEAU FICHIER)

package com.mediatower.backend.controller;

import com.mediatower.backend.dto.AdminReviewDto;
import com.mediatower.backend.service.AdminReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')") // Sécurise toutes les actions pour les administrateurs
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping
    public ResponseEntity<List<AdminReviewDto>> getAllReviews() {
        return ResponseEntity.ok(adminReviewService.getAllReviewsUnified());
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String type, @PathVariable Long id) {
        try {
            adminReviewService.deleteReview(type, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}