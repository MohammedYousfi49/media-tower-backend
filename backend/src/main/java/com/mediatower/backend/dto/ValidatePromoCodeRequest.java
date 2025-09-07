// Fichier : src/main/java/com/mediatower/backend/dto/ValidatePromoCodeRequest.java (NOUVEAU)

package com.mediatower.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidatePromoCodeRequest {
    @NotBlank(message = "Promo code cannot be empty")
    private String code;
}