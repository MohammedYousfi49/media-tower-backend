// Fichier : src/main/java/com/mediatower/backend/service/ProductPackService.java (COMPLET ET MIS À JOUR)

package com.mediatower.backend.service;

import com.mediatower.backend.dto.MediaDto;
import com.mediatower.backend.dto.ProductPackDto;
import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.MediaType;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.model.ProductPack;
import com.mediatower.backend.repository.ProductPackRepository;
import com.mediatower.backend.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductPackService {

    private final ProductPackRepository packRepository;
    private final ProductRepository productRepository;
    private final ProductPackRepository productPackRepository;
    private final String baseUrl = "http://localhost:8080/api/download/";

    public ProductPackService(ProductPackRepository packRepository, ProductRepository productRepository, ProductPackRepository productPackRepository) {
        this.packRepository = packRepository;
        this.productRepository = productRepository;
        this.productPackRepository = productPackRepository;
    }

    public Page<ProductPackDto> getAllPacksPaginated(String searchTerm, Pageable pageable) {
        String search = (searchTerm == null || searchTerm.trim().isEmpty()) ? null : searchTerm;
        Page<ProductPack> packPage = packRepository.findBySearchTerm(search, pageable);
        return packPage.map(this::convertToDto);
    }


    public Optional<ProductPackDto> getPackById(Long id) {
        return packRepository.findById(id).map(this::convertToDto);
    }

    @Transactional
    public ProductPackDto createPack(ProductPackDto dto) {
        ProductPack pack = new ProductPack();
        updatePackFromDto(pack, dto);
        return convertToDto(packRepository.save(pack));
    }

    @Transactional
    public ProductPackDto updatePack(Long id, ProductPackDto dto) {
        ProductPack pack = packRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pack not found with id: " + id));
        updatePackFromDto(pack, dto);
        return convertToDto(packRepository.save(pack));
    }

    private void updatePackFromDto(ProductPack pack, ProductPackDto dto) {
        if (dto.getProductIds() == null || dto.getProductIds().isEmpty()) {
            throw new IllegalArgumentException("A pack must contain at least one product.");
        }

        pack.setNames(dto.getNames());
        pack.setDescriptions(dto.getDescriptions());
        pack.setPrice(dto.getPrice());

        List<Product> products = productRepository.findAllById(dto.getProductIds());
        if (products.size() != dto.getProductIds().size()) {
            throw new RuntimeException("One or more products not found for the given IDs.");
        }
        pack.setProducts(new HashSet<>(products));
    }

    public void deletePack(Long id) {
        packRepository.deleteById(id);
    }

    private ProductPackDto convertToDto(ProductPack pack) {
        ProductPackDto dto = new ProductPackDto();
        dto.setId(pack.getId());
        dto.setNames(pack.getNames());
        dto.setDescriptions(pack.getDescriptions());
        dto.setPrice(pack.getPrice());

        if (pack.getMediaAssets() != null) {
            dto.setImages(pack.getMediaAssets().stream()
                    .map(this::convertMediaToDto)
                    .collect(Collectors.toList()));
        }

        if (pack.getProducts() != null) {
            dto.setProductIds(pack.getProducts().stream().map(Product::getId).collect(Collectors.toSet()));
        }
        return dto;
    }

    private MediaDto convertMediaToDto(Media media) {
        MediaDto mediaDto = new MediaDto();
        mediaDto.setId(media.getId());
        mediaDto.setFileName(media.getFileName());
        mediaDto.setOriginalName(media.getOriginalName());
        mediaDto.setType(media.getType().name());
        mediaDto.setUrl(baseUrl + media.getFileName());
        mediaDto.setPrimary(media.isPrimary());
        return mediaDto;
    }

    public List<ProductPackDto> getAllPacksForSelection() {
        return productPackRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}