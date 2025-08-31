package com.mediatower.backend.controller;

import com.mediatower.backend.dto.PromotionDto;
import com.mediatower.backend.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    // Admin: récupérer toutes les promotions
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromotionDto>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    // Admin: créer une nouvelle promotion
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionDto> createPromotion(@RequestBody PromotionDto dto) {
        // La méthode du service gère à la fois la création et la mise à jour,
        // donc on s'assure que l'ID est nul pour une création.
        dto.setId(null);
        PromotionDto createdPromotion = promotionService.createOrUpdatePromotion(dto);
        return new ResponseEntity<>(createdPromotion, HttpStatus.CREATED);
    }

    // Admin: mettre à jour une promotion existante
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionDto> updatePromotion(@PathVariable Long id, @RequestBody PromotionDto dto) {
        dto.setId(id); // On assigne l'ID de l'URL au DTO pour la mise à jour
        PromotionDto updatedPromotion = promotionService.createOrUpdatePromotion(dto);
        return ResponseEntity.ok(updatedPromotion);
    }

    // Admin: supprimer une promotion
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        try {
            promotionService.deletePromotion(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            // Gérer le cas où la promotion n'existe pas (par exemple)
            return ResponseEntity.notFound().build();
        }
    }
}