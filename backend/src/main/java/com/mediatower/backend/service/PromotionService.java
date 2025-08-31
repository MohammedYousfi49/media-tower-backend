// Fichier : src/main/java/com/mediatower/backend/service/PromotionService.java (COMPLET ET MIS À JOUR)

package com.mediatower.backend.service;

import com.mediatower.backend.dto.PromotionDto;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.ProductPackRepository;
import com.mediatower.backend.repository.ProductRepository;
import com.mediatower.backend.repository.PromotionRepository;
import com.mediatower.backend.repository.ServiceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final ProductPackRepository packRepository; // <-- AJOUT

    public PromotionService(PromotionRepository promotionRepository, ProductRepository productRepository, ServiceRepository serviceRepository, ProductPackRepository packRepository) { // <-- AJOUT
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.packRepository = packRepository; // <-- AJOUT
    }

    public List<PromotionDto> getAllPromotions() {
        return promotionRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public PromotionDto createOrUpdatePromotion(PromotionDto dto) {
        Promotion promotion;
        if (dto.getId() != null) {
            promotion = promotionRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Promotion not found"));
        } else {
            promotion = new Promotion();
        }

        String code = dto.getCode();
        promotion.setCode(code != null && !code.trim().isEmpty() ? code.toUpperCase() : null);

        promotion.setDescription(dto.getDescription());
        promotion.setDiscountType(DiscountType.valueOf(dto.getDiscountType()));
        promotion.setDiscountValue(dto.getDiscountValue());
        promotion.setStartDate(dto.getStartDate());
        promotion.setEndDate(dto.getEndDate());
        promotion.setActive(dto.isActive());

        promotion.getApplicableProducts().clear();
        if (dto.getApplicableProductIds() != null) {
            List<Product> products = productRepository.findAllById(dto.getApplicableProductIds());
            promotion.setApplicableProducts(new HashSet<>(products));
        }

        promotion.getApplicableServices().clear();
        if (dto.getApplicableServiceIds() != null) {
            List<Service> services = serviceRepository.findAllById(dto.getApplicableServiceIds());
            promotion.setApplicableServices(new HashSet<>(services));
        }

        // --- DÉBUT DE L'AJOUT ---
        promotion.getApplicablePacks().clear();
        if (dto.getApplicablePackIds() != null) {
            List<ProductPack> packs = packRepository.findAllById(dto.getApplicablePackIds());
            promotion.setApplicablePacks(new HashSet<>(packs));
        }
        // --- FIN DE L'AJOUT ---

        return convertToDto(promotionRepository.save(promotion));
    }

    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    private PromotionDto convertToDto(Promotion promotion) {
        PromotionDto dto = new PromotionDto();
        dto.setId(promotion.getId());
        dto.setCode(promotion.getCode());
        dto.setDescription(promotion.getDescription());
        dto.setDiscountType(promotion.getDiscountType().name());
        dto.setDiscountValue(promotion.getDiscountValue());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setActive(promotion.isActive());
        dto.setApplicableProductIds(promotion.getApplicableProducts().stream().map(Product::getId).collect(Collectors.toSet()));
        dto.setApplicableServiceIds(promotion.getApplicableServices().stream().map(Service::getId).collect(Collectors.toSet()));

        // --- DÉBUT DE L'AJOUT ---
        dto.setApplicablePackIds(promotion.getApplicablePacks().stream().map(ProductPack::getId).collect(Collectors.toSet()));
        // --- FIN DE L'AJOUT ---

        return dto;
    }
}