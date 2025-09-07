// Fichier : src/main/java/com/mediatower/backend/service/PromotionService.java (COMPLET ET PROFESSIONNEL)

package com.mediatower.backend.service;

import com.mediatower.backend.dto.PromotionDto;
import com.mediatower.backend.exception.InvalidPromotionException;
import com.mediatower.backend.exception.ResourceNotFoundException;
import com.mediatower.backend.model.DiscountType;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.model.ProductPack;
import com.mediatower.backend.model.Promotion;
import com.mediatower.backend.model.Service;
import com.mediatower.backend.repository.ProductPackRepository;
import com.mediatower.backend.repository.ProductRepository;
import com.mediatower.backend.repository.PromotionRepository;
import com.mediatower.backend.repository.ServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final ProductPackRepository packRepository;

    public PromotionService(PromotionRepository promotionRepository, ProductRepository productRepository, ServiceRepository serviceRepository, ProductPackRepository packRepository) {
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.packRepository = packRepository;
    }

    public List<PromotionDto> getAllPromotions() { return promotionRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList()); }
    @Transactional(readOnly = true) public PromotionDto validatePromotionCode(String code) { if (code == null || code.trim().isEmpty()) { throw new InvalidPromotionException("Code promotionnel requis."); } Promotion promotion = promotionRepository.findByCodeAndIsActiveTrue(code.toUpperCase()) .orElseThrow(() -> new ResourceNotFoundException("Code promotionnel invalide ou expiré.")); LocalDateTime now = LocalDateTime.now(); if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) { throw new InvalidPromotionException("Cette promotion n'a pas encore commencé."); } if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) { throw new InvalidPromotionException("Cette promotion a expiré."); } if (promotion.getDiscountType() == DiscountType.PERCENTAGE && promotion.getDiscountValue().compareTo(new BigDecimal("95")) > 0) { logger.warn("Promotion avec réduction > 95% détectée : {}", code); } return convertToDto(promotion); }


    @Transactional
    public PromotionDto createOrUpdatePromotion(PromotionDto dto) {
        Promotion promotion;
        if (dto.getId() != null) {
            promotion = promotionRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Promotion not found"));
        } else {
            // C'est une NOUVELLE promotion
            promotion = new Promotion();
            // ▼▼▼ LOGIQUE SIMPLIFIÉE ▼▼▼
            // Toute nouvelle promotion est active par défaut.
            promotion.setActive(true);
        }

        String code = dto.getCode();
        promotion.setCode(code != null && !code.trim().isEmpty() ? code.toUpperCase() : null);
        promotion.setDescription(dto.getDescription());
        promotion.setDiscountType(DiscountType.valueOf(dto.getDiscountType()));
        promotion.setDiscountValue(dto.getDiscountValue());
        promotion.setStartDate(dto.getStartDate());
        promotion.setEndDate(dto.getEndDate());

        // La logique pour les produits/services/packs reste la même
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
        promotion.getApplicablePacks().clear();
        if (dto.getApplicablePackIds() != null) {
            List<ProductPack> packs = packRepository.findAllById(dto.getApplicablePackIds());
            promotion.setApplicablePacks(new HashSet<>(packs));
        }

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
        // ▼▼▼ On utilise le setter qui correspond à notre DTO ▼▼▼
        dto.setActive(promotion.isActive());
        // ▲▲▲
        dto.setApplicableProductIds(promotion.getApplicableProducts().stream().map(Product::getId).collect(Collectors.toSet()));
        dto.setApplicableServiceIds(promotion.getApplicableServices().stream().map(Service::getId).collect(Collectors.toSet()));
        dto.setApplicablePackIds(promotion.getApplicablePacks().stream().map(ProductPack::getId).collect(Collectors.toSet()));
        return dto;
    }
}