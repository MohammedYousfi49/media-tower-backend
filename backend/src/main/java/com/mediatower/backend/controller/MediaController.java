// Fichier : src/main/java/com/mediatower/backend/controller/MediaController.java (NOUVEAU FICHIER)

package com.mediatower.backend.controller;

import com.mediatower.backend.service.MediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/media")
@PreAuthorize("hasRole('ADMIN')") // Sécurise toutes les méthodes de ce contrôleur
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/set-primary/product/{productId}/media/{mediaId}")
    public ResponseEntity<Void> setProductPrimaryMedia(@PathVariable Long productId, @PathVariable Long mediaId) {
        mediaService.setPrimaryMediaForProduct(productId, mediaId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/set-primary/service/{serviceId}/media/{mediaId}")
    public ResponseEntity<Void> setServicePrimaryMedia(@PathVariable Long serviceId, @PathVariable Long mediaId) {
        mediaService.setPrimaryMediaForService(serviceId, mediaId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/set-primary/pack/{packId}/media/{mediaId}")
    public ResponseEntity<Void> setPackPrimaryMedia(@PathVariable Long packId, @PathVariable Long mediaId) {
        mediaService.setPrimaryMediaForPack(packId, mediaId);
        return ResponseEntity.ok().build();
    }
}