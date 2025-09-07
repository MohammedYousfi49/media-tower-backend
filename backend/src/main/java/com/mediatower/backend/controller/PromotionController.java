// Fichier : src/main/java/com/mediatower/backend/controller/PromotionController.java (COMPLET ET SIMPLIFIÉ)

package com.mediatower.backend.controller;

import com.mediatower.backend.dto.PromotionDto;
import com.mediatower.backend.dto.ValidatePromoCodeRequest;
import com.mediatower.backend.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// L'import @PreAuthorize a été retiré.
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    // Sécurité gérée par SecurityConfig
    @GetMapping
    public ResponseEntity<List<PromotionDto>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    // Sécurité gérée par SecurityConfig
    @PostMapping
    public ResponseEntity<PromotionDto> createPromotion(@RequestBody PromotionDto dto) {
        dto.setId(null);
        PromotionDto createdPromotion = promotionService.createOrUpdatePromotion(dto);
        return new ResponseEntity<>(createdPromotion, HttpStatus.CREATED);
    }

    // Public (sécurité gérée par SecurityConfig)
    @PostMapping("/validate")
    public ResponseEntity<PromotionDto> validatePromotionCode(@Valid @RequestBody ValidatePromoCodeRequest request) {
        PromotionDto validPromotion = promotionService.validatePromotionCode(request.getCode());
        return ResponseEntity.ok(validPromotion);
    }

    // Sécurité gérée par SecurityConfig
    @PutMapping("/{id}")
    public ResponseEntity<PromotionDto> updatePromotion(@PathVariable Long id, @RequestBody PromotionDto dto) {
        dto.setId(id);
        PromotionDto updatedPromotion = promotionService.createOrUpdatePromotion(dto);
        return ResponseEntity.ok(updatedPromotion);
    }

    // Sécurité gérée par SecurityConfig
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}