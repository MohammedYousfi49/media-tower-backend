package com.mediatower.backend.controller;

import com.mediatower.backend.dto.AdminReviewDto;
import com.mediatower.backend.service.AdminReviewService;
import com.mediatower.backend.service.ReviewService;
import com.mediatower.backend.service.ServiceReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;
    private final ReviewService productReviewService;
    private final ServiceReviewService serviceReviewService;

    public AdminReviewController(AdminReviewService adminReviewService, ReviewService productReviewService, ServiceReviewService serviceReviewService) {
        this.adminReviewService = adminReviewService;
        this.productReviewService = productReviewService;
        this.serviceReviewService = serviceReviewService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminReviewDto>> getAllReviews(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<AdminReviewDto> reviews = adminReviewService.getAllReviews(search, pageable);
        return ResponseEntity.ok(reviews);
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<Void> deleteProductReview(@PathVariable Long id) {
        productReviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/service/{id}")
    public ResponseEntity<Void> deleteServiceReview(@PathVariable Long id) {
        serviceReviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}