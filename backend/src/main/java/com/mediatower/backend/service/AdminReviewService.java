// Fichier : src/main/java/com/mediatower/backend/service/AdminReviewService.java (NOUVEAU FICHIER)

package com.mediatower.backend.service;

import com.mediatower.backend.dto.AdminReviewDto;
import com.mediatower.backend.model.Review;
import com.mediatower.backend.model.ServiceReview;
import com.mediatower.backend.repository.ReviewRepository;
import com.mediatower.backend.repository.ServiceReviewRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final ServiceReviewRepository serviceReviewRepository;

    public AdminReviewService(ReviewRepository reviewRepository, ServiceReviewRepository serviceReviewRepository) {
        this.reviewRepository = reviewRepository;
        this.serviceReviewRepository = serviceReviewRepository;
    }

    public List<AdminReviewDto> getAllReviewsUnified() {
        // 1. Récupérer tous les avis sur les produits
        List<AdminReviewDto> productReviews = reviewRepository.findAll().stream()
                .map(this::convertProductReviewToAdminDto)
                .collect(Collectors.toList());

        // 2. Récupérer tous les avis sur les services
        List<AdminReviewDto> serviceReviews = serviceReviewRepository.findAll().stream()
                .map(this::convertServiceReviewToAdminDto)
                .collect(Collectors.toList());

        // 3. Fusionner les deux listes
        List<AdminReviewDto> allReviews = new ArrayList<>();
        allReviews.addAll(productReviews);
        allReviews.addAll(serviceReviews);

        // 4. Trier par date, du plus récent au plus ancien
        allReviews.sort(Comparator.comparing(AdminReviewDto::getReviewDate).reversed());

        return allReviews;
    }

    public void deleteReview(String type, Long id) {
        if ("product".equalsIgnoreCase(type)) {
            reviewRepository.deleteById(id);
        } else if ("service".equalsIgnoreCase(type)) {
            serviceReviewRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Invalid review type specified: " + type);
        }
    }

    // Méthodes de conversion
    private AdminReviewDto convertProductReviewToAdminDto(Review review) {
        AdminReviewDto dto = new AdminReviewDto();
        dto.setId(review.getId());
        dto.setType("Product");
        dto.setSourceId(review.getProduct().getId());
        dto.setSourceName(review.getProduct().getNames().getOrDefault("en", "N/A"));
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewDate(review.getReviewDate());
        return dto;
    }

    private AdminReviewDto convertServiceReviewToAdminDto(ServiceReview review) {
        AdminReviewDto dto = new AdminReviewDto();
        dto.setId(review.getId());
        dto.setType("Service");
        dto.setSourceId(review.getService().getId());
        dto.setSourceName(review.getService().getNames().getOrDefault("en", "N/A"));
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewDate(review.getReviewDate());
        return dto;
    }
}