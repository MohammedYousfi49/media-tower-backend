// Fichier : src/main/java/com/mediatower/backend/dto/PromotionDto.java (COMPLET ET MIS À JOUR)

package com.mediatower.backend.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PromotionDto {
    private Long id;
    private String code, description, discountType;
    private BigDecimal discountValue;
    private LocalDateTime startDate, endDate;
    @JsonProperty("isActive")
    private boolean active;
    private Set<Long> applicableProductIds, applicableServiceIds;

    // --- DÉBUT DE L'AJOUT ---
    private Set<Long> applicablePackIds;
    // --- FIN DE L'AJOUT ---
}