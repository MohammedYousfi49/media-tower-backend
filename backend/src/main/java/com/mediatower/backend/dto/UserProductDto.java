package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProductDto {
    private Long productId;
    private Map<String, String> names;
    // URL de la miniature du produit (première image)
    private String thumbnailUrl;
    private LocalDateTime purchaseDate;
    private LocalDateTime accessExpiresAt;
    private int downloadCount;
}