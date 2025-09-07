package com.mediatower.backend.service;

import com.mediatower.backend.dto.AdminReviewDto;
import com.mediatower.backend.repository.ReviewRepository;
import com.mediatower.backend.repository.ServiceReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminReviewService {

    private final ReviewRepository productReviewRepository;
    private final ServiceReviewRepository serviceReviewRepository;

    public AdminReviewService(ReviewRepository productReviewRepository, ServiceReviewRepository serviceReviewRepository) {
        this.productReviewRepository = productReviewRepository;
        this.serviceReviewRepository = serviceReviewRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminReviewDto> getAllReviews(String searchTerm, Pageable pageable) {
        // 1. Récupérer tous les avis sans pagination initiale
        List<AdminReviewDto> combinedReviews = new ArrayList<>();

        productReviewRepository.findAll().forEach(review -> {
            AdminReviewDto dto = new AdminReviewDto();
            dto.setId(review.getId());
            dto.setType("Product");
            dto.setSourceId(review.getProduct().getId());
            dto.setSourceName(review.getProduct().getNames().getOrDefault("en", "N/A"));
            dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
            dto.setUserEmail(review.getUser().getEmail()); // Ajout de l'email
            dto.setRating(review.getRating());
            dto.setComment(review.getComment());
            dto.setReviewDate(review.getReviewDate());
            combinedReviews.add(dto);
        });

        serviceReviewRepository.findAll().forEach(review -> {
            AdminReviewDto dto = new AdminReviewDto();
            dto.setId(review.getId());
            dto.setType("Service");
            dto.setSourceId(review.getService().getId());
            dto.setSourceName(review.getService().getNames().getOrDefault("en", "N/A"));
            dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
            dto.setUserEmail(review.getUser().getEmail()); // Ajout de l'email
            dto.setRating(review.getRating());
            dto.setComment(review.getComment());
            dto.setReviewDate(review.getReviewDate());
            combinedReviews.add(dto);
        });

        // 2. Trier la liste combinée par date (plus récent en premier)
        combinedReviews.sort(Comparator.comparing(AdminReviewDto::getReviewDate).reversed());

        // 3. Appliquer le filtre de recherche manuellement
        List<AdminReviewDto> filteredReviews;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String lowerCaseSearchTerm = searchTerm.toLowerCase();
            filteredReviews = combinedReviews.stream()
                    .filter(review -> review.getSourceName().toLowerCase().contains(lowerCaseSearchTerm) ||
                            review.getUserName().toLowerCase().contains(lowerCaseSearchTerm) ||
                            review.getUserEmail().toLowerCase().contains(lowerCaseSearchTerm) ||
                            (review.getComment() != null && review.getComment().toLowerCase().contains(lowerCaseSearchTerm)))
                    .collect(Collectors.toList());
        } else {
            filteredReviews = combinedReviews;
        }

        // 4. Appliquer la pagination manuellement
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredReviews.size());

        List<AdminReviewDto> pageContent = (start > filteredReviews.size()) ? List.of() : filteredReviews.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredReviews.size());
    }
}