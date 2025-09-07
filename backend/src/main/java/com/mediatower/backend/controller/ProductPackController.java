// Fichier : src/main/java/com/mediatower/backend/controller/ProductPackController.java (COMPLET ET CORRIGÉ)

package com.mediatower.backend.controller;

import com.mediatower.backend.dto.ProductPackDto;
import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.MediaType;
import com.mediatower.backend.model.ProductPack;
import com.mediatower.backend.repository.MediaRepository;
import com.mediatower.backend.repository.ProductPackRepository;
import com.mediatower.backend.service.FileStorageService;
import com.mediatower.backend.service.ProductPackService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/packs")
public class ProductPackController {

    private final ProductPackService packService;
    private final ProductPackRepository packRepository;
    private final FileStorageService fileStorageService;
    private final MediaRepository mediaRepository;

    public ProductPackController(ProductPackService packService, ProductPackRepository packRepository, FileStorageService fileStorageService, MediaRepository mediaRepository) {
        this.packService = packService;
        this.packRepository = packRepository;
        this.fileStorageService = fileStorageService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping
    public ResponseEntity<Page<ProductPackDto>> getAllPacks(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<ProductPackDto> packs = packService.getAllPacksPaginated(search, pageable);
        return ResponseEntity.ok(packs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductPackDto> getPackById(@PathVariable Long id) {
        return packService.getPackById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CORRECTION: Une seule méthode /all pour admin
    @GetMapping("/all")
    public ResponseEntity<List<ProductPackDto>> getAllPacksForAdmin() {
        return ResponseEntity.ok(packService.getAllPacksForSelection());
    }

    @PostMapping(consumes = { "multipart/form-data" })
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductPackDto> createPack(
            @Valid @RequestPart("packDto") ProductPackDto dto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images) {
        ProductPackDto createdPackDto = packService.createPack(dto);
        ProductPack pack = packRepository.findById(createdPackDto.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve pack after creation"));

        associateMedia(pack, images);

        ProductPackDto finalDto = packService.getPackById(pack.getId())
                .orElseThrow(() -> new RuntimeException("Failed to refetch pack DTO"));
        return new ResponseEntity<>(finalDto, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductPackDto> updatePack(
            @PathVariable Long id,
            @Valid @RequestPart("packDto") ProductPackDto dto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images) {
        ProductPackDto updatedPackDto = packService.updatePack(id, dto);
        ProductPack pack = packRepository.findById(updatedPackDto.getId())
                .orElseThrow(() -> new RuntimeException("Pack not found after update"));

        associateMedia(pack, images);

        ProductPackDto finalDto = packService.getPackById(pack.getId())
                .orElseThrow(() -> new RuntimeException("Failed to refetch pack DTO"));
        return ResponseEntity.ok(finalDto);
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePack(@PathVariable Long id) {
        packService.deletePack(id);
        return ResponseEntity.noContent().build();
    }

    private void associateMedia(ProductPack pack, List<MultipartFile> images) {
        if (images != null && !images.isEmpty()) {
            boolean isFirstImage = !pack.getMediaAssets().stream().anyMatch(m -> m.isPrimary() && m.getType() == MediaType.IMAGE);
            for (MultipartFile file : images) {
                String fileName = fileStorageService.storeFile(file);
                Media media = new Media();
                media.setFileName(fileName);
                media.setOriginalName(file.getOriginalFilename());
                media.setType(MediaType.IMAGE);
                media.setPack(pack);
                if (isFirstImage) {
                    media.setPrimary(true);
                    isFirstImage = false;
                }
                mediaRepository.save(media);
            }
        }
    }
}