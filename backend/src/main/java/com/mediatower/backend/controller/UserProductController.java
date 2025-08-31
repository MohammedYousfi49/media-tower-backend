package com.mediatower.backend.controller;

import com.mediatower.backend.dto.UserProductDto;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.UserProductAccessRepository;
import com.mediatower.backend.repository.UserRepository;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.FileStorageService;
import com.mediatower.backend.service.S3Service;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserProductController {

    private final UserProductAccessRepository userProductAccessRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final FileStorageService fileStorageService;

    public UserProductController(UserProductAccessRepository userProductAccessRepository,
                                 UserRepository userRepository,
                                 S3Service s3Service,
                                 FileStorageService fileStorageService) {
        this.userProductAccessRepository = userProductAccessRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/user/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserProductDto>> getMyPurchasedProducts(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        User user = userRepository.findByUid(firebaseUser.getUid())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<UserProductDto> products = userProductAccessRepository.findByUser(user)
                .stream()
                .map(this::convertToUserProductDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{productId}/download-link")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> getDownloadLink(@PathVariable Long productId, @AuthenticationPrincipal FirebaseUser firebaseUser) {
        User user = userRepository.findByUid(firebaseUser.getUid())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        UserProductAccess access = userProductAccessRepository.findByUserAndProductId(user, productId)
                .orElseThrow(() -> new SecurityException("User does not have access to this product"));

        if (access.getAccessExpiresAt() != null && access.getAccessExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(403).body(Map.of("error", "Your access to this product has expired."));
        }

        Product product = access.getProduct();
        if (product.getS3ObjectKey() == null || product.getS3ObjectKey().isBlank()) {
            return ResponseEntity.status(500).body(Map.of("error", "Product content is not available for download. Please contact support."));
        }

        access.setDownloadCount(access.getDownloadCount() + 1);
        access.setLastDownloadAt(LocalDateTime.now());
        userProductAccessRepository.save(access);

        URL presignedUrl = s3Service.generatePresignedDownloadUrl(product.getS3ObjectKey());

        return ResponseEntity.ok(Map.of("downloadUrl", presignedUrl.toString()));
    }

    private UserProductDto convertToUserProductDto(UserProductAccess access) {
        Product product = access.getProduct();

        // ================== CORRECTION DE L'IMAGE MANQUANTE ==================
        // On construit l'URL de la même manière que dans ProductService pour la cohérence
        String baseUrl = "http://localhost:8080/api/download/";
        String thumbnailUrl = product.getMediaAssets().stream()
                .filter(media -> media.getType() == MediaType.IMAGE && media.isPrimary())
                .findFirst()
                .map(media -> baseUrl + media.getFileName()) // On utilise la construction manuelle
                .orElse(null);
        // S'il n'y a pas d'image primaire, on prend la première de la liste
        if (thumbnailUrl == null && product.getMediaAssets() != null && !product.getMediaAssets().isEmpty()) {
            thumbnailUrl = product.getMediaAssets().stream()
                    .filter(media -> media.getType() == MediaType.IMAGE)
                    .findFirst()
                    .map(media -> baseUrl + media.getFileName())
                    .orElse(null);
        }
        // ===================================================================

        return new UserProductDto(
                product.getId(),
                product.getNames(),
                thumbnailUrl,
                access.getPurchaseDate(),
                access.getAccessExpiresAt(),
                access.getDownloadCount()
        );
    }
}