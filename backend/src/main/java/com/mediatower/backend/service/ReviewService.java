package com.mediatower.backend.service;

import com.mediatower.backend.dto.ReviewDto;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.model.Review;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.ProductRepository;
import com.mediatower.backend.repository.ReviewRepository;
import com.mediatower.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository, ProductRepository productRepository, OrderService orderService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderService = orderService; // << MISE À JOUR DU CONSTRUCTEUR
    }

    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ReviewDto> getReviewsByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
        return reviewRepository.findByProduct(product).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ReviewDto> getReviewById(Long id) {
        return reviewRepository.findById(id).map(this::convertToDto);
    }

    @Transactional // Ajouter @Transactional pour la cohérence
    public ReviewDto createReview(String userId, Long productId, ReviewDto reviewDto) {
        User user = userRepository.findByUid(userId)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        // --- VÉRIFICATION DE SÉCURITÉ ---
        // On vérifie si l'utilisateur a bien acheté le produit avant de permettre l'avis.
        if (!orderService.canUserReviewProduct(userId, productId)) {
            throw new IllegalStateException("User has not purchased this product and cannot leave a review.");
        }
        // --- FIN DE LA VÉRIFICATION ---

        if (reviewRepository.findByUserAndProduct(user, product).isPresent()) {
            throw new IllegalArgumentException("User has already reviewed this product.");
        }

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());

        return convertToDto(reviewRepository.save(review));
    }

    public ReviewDto updateReview(Long reviewId, ReviewDto reviewDto) {
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + reviewId));

        existingReview.setRating(reviewDto.getRating());
        existingReview.setComment(reviewDto.getComment());

        return convertToDto(reviewRepository.save(existingReview));
    }

    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Review not found with ID: " + id);
        }
        reviewRepository.deleteById(id);
    }
    public List<ReviewDto> getReviewsByUserId(String userId) {
        User user = userRepository.findByUid(userId)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userId));
        return reviewRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ReviewDto convertToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewDate(review.getReviewDate());
        return dto;
    }
}