// Fichier : src/main/java/com/mediatower/backend/dto/AdminReviewDto.java (NOUVEAU FICHIER)

package com.mediatower.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminReviewDto {
    private Long id;
    private String type; // "Product" ou "Service"
    private Long sourceId; // ID du produit ou du service
    private String sourceName; // Nom du produit ou du service
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime reviewDate;
}