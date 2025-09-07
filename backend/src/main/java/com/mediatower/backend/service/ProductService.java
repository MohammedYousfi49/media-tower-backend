package com.mediatower.backend.service;

import com.mediatower.backend.dto.MediaDto;
import com.mediatower.backend.dto.ProductDto;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final TagRepository tagRepository;
    private final MediaRepository mediaRepository;
    private final OrderRepository orderRepository;
    private final S3Service s3Service; // Assurez-vous d'injecter S3Service
    private final UserProductAccessRepository userProductAccessRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, OrderItemRepository orderItemRepository, TagRepository tagRepository, MediaRepository mediaRepository, OrderRepository orderRepository, S3Service s3Service, UserProductAccessRepository userProductAccessRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.tagRepository = tagRepository;
        this.mediaRepository = mediaRepository;
        this.orderRepository = orderRepository;
        this.s3Service = s3Service;
        this.userProductAccessRepository = userProductAccessRepository;
    }

    public Page<ProductDto> getAllProductsPaginated(String searchTerm, Long categoryId, String stockStatus, Pageable pageable) {
        // Normalisation des filtres pour la requête
        String search = (searchTerm == null || searchTerm.trim().isEmpty()) ? null : searchTerm;
        Long catId = (categoryId != null && categoryId == 0) ? null : categoryId; // On considère 0 comme 'all'
        String stock = (stockStatus == null || stockStatus.equals("all")) ? null : stockStatus;

        Page<Product> productPage = productRepository.findWithFilters(search, catId, stock, pageable);
        return productPage.map(this::convertToDto);
    }


    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findById(id).map(this::convertToDto);

    }

//
public List<ProductDto> getAllProductsForSelection() {
    return productRepository.findAll().stream()
            .map(product -> {
                ProductDto dto = new ProductDto();
                dto.setId(product.getId());
                dto.setNames(product.getNames());
                dto.setPrice(product.getPrice());
                return dto;
            })
            .collect(Collectors.toList());
}

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product product = new Product();
        updateProductFromDto(product, dto);
        return convertToDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        updateProductFromDto(product, dto);
        return convertToDto(productRepository.save(product));
    }

    private void updateProductFromDto(Product product, ProductDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        product.setNames(dto.getNames());
        product.setDescriptions(dto.getDescriptions());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setCategory(category);

        if (dto.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            product.setTags(new HashSet<>(tags));
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (orderRepository.countByOrderItemsProductId(id) > 0) {
            throw new IllegalStateException("Cannot delete product: It is part of one or more existing orders.");
        }
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(id);
    }

    public List<ProductDto> getPopularProducts(int limit) {
        return Collections.emptyList(); // Placeholder
    }



    public List<ProductDto> getSimilarProducts(Long productId) {
        return Collections.emptyList(); // Placeholder
    }

    public List<ProductDto> searchProducts(String name, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        return Collections.emptyList(); // Placeholder
    }

    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            // ================== CORRECTION POUR "UNCATEGORIZED" ==================
            // On vérifie que la map de noms n'est pas nulle avant de chercher une clé
            if (product.getCategory().getNames() != null) {
                dto.setCategoryName(product.getCategory().getNames().getOrDefault("en", "No Category Name"));
            }
            // ====================================================================
        }

        dto.setNames(product.getNames());
        dto.setDescriptions(product.getDescriptions());

        if (product.getTags() != null) {
            dto.setTagIds(product.getTags().stream().map(Tag::getId).collect(Collectors.toSet()));
        }

        String baseUrl = "http://localhost:8080/api/download/";

        if (product.getMediaAssets() != null) {
            dto.setImages(product.getMediaAssets().stream()
                    .filter(media -> media.getType() == MediaType.IMAGE)
                    .map(media -> convertMediaToDto(media, baseUrl))
                    .collect(Collectors.toList()));
            dto.setDigitalAssets(product.getMediaAssets().stream()
                    .filter(media -> media.getType() == MediaType.DIGITAL_ASSET)
                    .map(media -> convertMediaToDto(media, baseUrl))
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private MediaDto convertMediaToDto(Media media, String baseUrl) {
        MediaDto mediaDto = new MediaDto();
        mediaDto.setId(media.getId());
        mediaDto.setFileName(media.getFileName());
        mediaDto.setOriginalName(media.getOriginalName());
        mediaDto.setType(media.getType().name());
        mediaDto.setUrl(baseUrl + media.getFileName());
        mediaDto.setPrimary(media.isPrimary());
        return mediaDto;
    }
    @Transactional(readOnly = true)
    public String generatePresignedDownloadLink(Long productId, String userUid) {
        // 1. ▼▼▼ CORRECTION DU NOM DE LA MÉTHODE ▼▼▼
        // La méthode correcte est 'existsByUserUidAndProductId'
        boolean hasAccess = userProductAccessRepository.existsByUserUidAndProductId(userUid, productId);
        if (!hasAccess) {
            throw new SecurityException("User does not have access to this product.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getS3ObjectKey() == null || product.getS3ObjectKey().isBlank()) {
            throw new RuntimeException("This product has no downloadable file.");
        }

        // 2. ▼▼▼ CORRECTION DU NOM DE LA MÉTHODE S3 ▼▼▼
        // La méthode correcte dans votre S3Service est 'generatePresignedDownloadUrl'
        return s3Service.generatePresignedDownloadUrl(product.getS3ObjectKey()).toString();
    }
}