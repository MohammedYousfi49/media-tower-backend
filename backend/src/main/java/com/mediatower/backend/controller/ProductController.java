package com.mediatower.backend.controller;

import com.mediatower.backend.dto.ProductDto;
import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.MediaType;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.repository.MediaRepository;
import com.mediatower.backend.repository.ProductRepository;
import com.mediatower.backend.service.FileStorageService;
import com.mediatower.backend.service.ProductService;
import com.mediatower.backend.service.S3Service; // <-- IMPORT NÉCESSAIRE
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException; // <-- IMPORT NÉCESSAIRE
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService; // Gardé pour les images
    private final MediaRepository mediaRepository;
    private final S3Service s3Service; // <-- INJECTION DU SERVICE S3

    // MISE À JOUR DU CONSTRUCTEUR
    public ProductController(ProductService productService, ProductRepository productRepository, FileStorageService fileStorageService, MediaRepository mediaRepository, S3Service s3Service) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.fileStorageService = fileStorageService;
        this.mediaRepository = mediaRepository;
        this.s3Service = s3Service;
    }

    // ========================================================================
    // == VOS MÉTHODES GET RESTENT INCHANGÉES ==
    // ========================================================================
    @GetMapping
    public List<ProductDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/search")
    public List<ProductDto> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.searchProducts(name, categoryId, minPrice, maxPrice);
    }

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

    // ========================================================================
    // == LES MÉTHODES DE CRÉATION/MISE À JOUR/SUPPRESSION SONT MODIFIÉES ==
    // ========================================================================

    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')") // Garder hasRole est correct pour votre projet
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestPart("productDto") ProductDto productDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images,
            @RequestPart(value = "newDigitalAssets", required = false) List<MultipartFile> digitalAssets
    ) throws IOException { // Ajout de throws IOException
        // 1. Créer le produit en base pour obtenir un ID
        ProductDto createdProductDto = productService.createProduct(productDto);
        Product product = productRepository.findById(createdProductDto.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve product after creation"));

        // 2. Gérer le téléversement du fichier numérique vers S3
        if (digitalAssets != null && !digitalAssets.isEmpty()) {
            MultipartFile mainAsset = digitalAssets.get(0); // On prend le premier fichier
            String s3Key = s3Service.uploadFile(mainAsset, "produits");
            product.setS3ObjectKey(s3Key);
            productRepository.save(product); // On sauvegarde la référence S3
        }

        // 3. La méthode associateMedia ne gère plus que les images
        associateImages(product, images);
        return new ResponseEntity<>(productService.getProductById(product.getId()).get(), HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestPart("productDto") ProductDto productDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images,
            @RequestPart(value = "newDigitalAssets", required = false) List<MultipartFile> digitalAssets
    ) throws IOException { // Ajout de throws IOException
        ProductDto updatedDto = productService.updateProduct(id, productDto);
        Product product = productRepository.findById(updatedDto.getId())
                .orElseThrow(() -> new RuntimeException("Product not found after update"));

        // Gérer le remplacement du fichier sur S3
        if (digitalAssets != null && !digitalAssets.isEmpty()) {
            // Supprimer l'ancien fichier pour ne pas laisser d'orphelins
            if (product.getS3ObjectKey() != null && !product.getS3ObjectKey().isBlank()) {
                s3Service.deleteFile(product.getS3ObjectKey());
            }
            // Téléverser le nouveau
            MultipartFile mainAsset = digitalAssets.get(0);
            String newS3Key = s3Service.uploadFile(mainAsset, "produits");
            product.setS3ObjectKey(newS3Key);
            productRepository.save(product);
        }

        associateImages(product, images);
        return ResponseEntity.ok(productService.getProductById(product.getId()).get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        // Supprimer le fichier S3 avant de supprimer le produit de la base
        productRepository.findById(id).ifPresent(product -> {
            if (product.getS3ObjectKey() != null && !product.getS3ObjectKey().isBlank()) {
                s3Service.deleteFile(product.getS3ObjectKey());
            }
        });
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // CETTE MÉTHODE EST MAINTENANT SIMPLIFIÉE POUR NE GÉRER QUE LES IMAGES
    private void associateImages(Product product, List<MultipartFile> images) {
        if (images != null && !images.isEmpty()) {
            boolean isFirstImage = !product.getMediaAssets().stream().anyMatch(m -> m.isPrimary() && m.getType() == MediaType.IMAGE);
            for (MultipartFile file : images) {
                // On utilise fileStorageService pour les images, comme avant
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