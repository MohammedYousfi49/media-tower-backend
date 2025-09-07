package com.mediatower.backend.controller;
import com.mediatower.backend.dto.ProductDto;
import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.MediaType;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.repository.MediaRepository;
import com.mediatower.backend.repository.ProductRepository;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.FileStorageService;
import com.mediatower.backend.service.ProductService;
import com.mediatower.backend.service.S3Service;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
    private final MediaRepository mediaRepository;
    private final S3Service s3Service;

    public ProductController(ProductService productService, ProductRepository productRepository, FileStorageService fileStorageService, MediaRepository mediaRepository, S3Service s3Service) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
        this.mediaRepository = mediaRepository;
        this.s3Service = s3Service;
    }

    @GetMapping
    public ResponseEntity<Page<ProductDto>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String stockStatus,
            Pageable pageable) {
        Page<ProductDto> products = productService.getAllProductsPaginated(search, categoryId, stockStatus, pageable);
        return ResponseEntity.ok(products);
    }

    // CORRECTION: Une seule méthode /all avec sécurité conditionnelle
    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProductsForAdmin() {
        List<ProductDto> products = productService.getAllProductsForSelection();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public List<ProductDto> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.searchProducts(name, categoryId, minPrice, maxPrice);
    }
//    @GetMapping("/{id}/download-link")
//    public ResponseEntity<?> getProductDownloadLink(
//            @PathVariable Long id,
//            @AuthenticationPrincipal FirebaseUser firebaseUser) {
//
//        if (firebaseUser == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
//        }
//
//        try {
//            String downloadUrl = productService.generatePresignedDownloadLink(id, firebaseUser.getUid());
//            return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
//        } catch (SecurityException e) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
//        }
//    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/popular")
    public ResponseEntity<List<ProductDto>> getPopularProducts() {
        return ResponseEntity.ok(productService.getPopularProducts(5));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<ProductDto>> getSimilarProducts(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getSimilarProducts(id));
    }

    @PostMapping(consumes = {"multipart/form-data"})
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestPart("productDto") ProductDto productDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images,
            @RequestPart(value = "newDigitalAssets", required = false) List<MultipartFile> digitalAssets
    ) throws IOException {
        ProductDto createdProductDto = productService.createProduct(productDto);
        Product product = productRepository.findById(createdProductDto.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve product after creation"));

        if (digitalAssets != null && !digitalAssets.isEmpty()) {
            MultipartFile mainAsset = digitalAssets.get(0);
            String s3Key = s3Service.uploadFile(mainAsset, "produits");
            product.setS3ObjectKey(s3Key);
            productRepository.save(product);
        }

        associateImages(product, images);
        return new ResponseEntity<>(productService.getProductById(product.getId()).get(), HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestPart("productDto") ProductDto productDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images,
            @RequestPart(value = "newDigitalAssets", required = false) List<MultipartFile> digitalAssets
    ) throws IOException {
        ProductDto updatedDto = productService.updateProduct(id, productDto);
        Product product = productRepository.findById(updatedDto.getId())
                .orElseThrow(() -> new RuntimeException("Product not found after update"));

        if (digitalAssets != null && !digitalAssets.isEmpty()) {
            if (product.getS3ObjectKey() != null && !product.getS3ObjectKey().isBlank()) {
                s3Service.deleteFile(product.getS3ObjectKey());
            }
            MultipartFile mainAsset = digitalAssets.get(0);
            String newS3Key = s3Service.uploadFile(mainAsset, "produits");
            product.setS3ObjectKey(newS3Key);
            productRepository.save(product);
        }

        associateImages(product, images);
        return ResponseEntity.ok(productService.getProductById(product.getId()).get());
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productRepository.findById(id).ifPresent(product -> {
            if (product.getS3ObjectKey() != null && !product.getS3ObjectKey().isBlank()) {
                s3Service.deleteFile(product.getS3ObjectKey());
            }
        });
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private void associateImages(Product product, List<MultipartFile> images) {
        if (images != null && !images.isEmpty()) {
            boolean isFirstImage = !product.getMediaAssets().stream().anyMatch(m -> m.isPrimary() && m.getType() == MediaType.IMAGE);
            for (MultipartFile file : images) {
                String fileName = fileStorageService.storeFile(file);
                Media media = new Media();
                media.setFileName(fileName);
                media.setOriginalName(file.getOriginalFilename());
                media.setType(MediaType.IMAGE);
                media.setProduct(product);
                if (isFirstImage) {
                    media.setPrimary(true);
                    isFirstImage = false;
                }
                mediaRepository.save(media);
            }
        }
    }
}