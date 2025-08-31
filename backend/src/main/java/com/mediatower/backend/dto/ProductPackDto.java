package com.mediatower.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ProductPackDto {
    private Long id;
    // Noms et descriptions multilingues
    private Map<String, String> names;
    private Map<String, String> descriptions;
    private BigDecimal price;
    // Une liste de DTO pour les images
    private List<MediaDto> images;
    // La liste des IDs des produits inclus
    private Set<Long> productIds;
}