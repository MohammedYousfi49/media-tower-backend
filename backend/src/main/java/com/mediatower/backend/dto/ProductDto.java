package com.mediatower.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ProductDto {
    private Long id;
    @NotNull private Map<String, String> names;
    private Map<String, String> descriptions;
    @NotNull @Positive private BigDecimal price;
    private Integer stock;
    @NotNull private Long categoryId;
    private String categoryName;
    private Set<Long> tagIds;

    // Remplacement des anciens champs par des listes de DTOs
    private List<MediaDto> images;
    private List<MediaDto> digitalAssets;
}