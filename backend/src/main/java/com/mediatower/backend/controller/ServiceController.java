// Fichier : src/main/java/com/mediatower/backend/controller/ServiceController.java (COMPLET ET FINAL)

package com.mediatower.backend.controller;

import com.mediatower.backend.dto.ServiceDto;
import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.MediaType;
import com.mediatower.backend.model.Service;
import com.mediatower.backend.repository.MediaRepository;
import com.mediatower.backend.repository.ServiceRepository;
import com.mediatower.backend.service.FileStorageService;
import com.mediatower.backend.service.ServiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;
    private final ServiceRepository serviceRepository;
    private final FileStorageService fileStorageService;
    private final MediaRepository mediaRepository;

    public ServiceController(ServiceService serviceService, ServiceRepository serviceRepository, FileStorageService fileStorageService, MediaRepository mediaRepository) {
        this.serviceService = serviceService;
        this.serviceRepository = serviceRepository;
        this.fileStorageService = fileStorageService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        return ResponseEntity.ok(serviceService.getAllServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable Long id) {
        return serviceService.getServiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDto> createService(
            @Valid @RequestPart("serviceDto") ServiceDto serviceDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images) {

        ServiceDto createdServiceDto = serviceService.createService(serviceDto);
        Service service = serviceRepository.findById(createdServiceDto.getId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve service after creation"));

        associateMedia(service, images);

        ServiceDto resultDto = serviceService.getServiceById(service.getId())
                .orElseThrow(() -> new RuntimeException("Could not refetch service DTO"));
        return new ResponseEntity<>(resultDto, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDto> updateService(
            @PathVariable Long id,
            @Valid @RequestPart("serviceDto") ServiceDto serviceDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images) {

        ServiceDto updatedDto = serviceService.updateService(id, serviceDto);
        Service service = serviceRepository.findById(updatedDto.getId())
                .orElseThrow(() -> new RuntimeException("Service not found after update"));

        associateMedia(service, images);

        ServiceDto resultDto = serviceService.getServiceById(service.getId())
                .orElseThrow(() -> new RuntimeException("Could not refetch service DTO"));
        return ResponseEntity.ok(resultDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<ServiceDto>> getSimilarServices(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getSimilarServices(id));
    }

    private void associateMedia(Service service, List<MultipartFile> images) {
        if (images != null && !images.isEmpty()) {
            boolean isFirstImage = !service.getMediaAssets().stream().anyMatch(m -> m.isPrimary() && m.getType() == MediaType.IMAGE);
            for (MultipartFile file : images) {
                String fileName = fileStorageService.storeFile(file);
                Media media = new Media();
                media.setFileName(fileName);
                media.setOriginalName(file.getOriginalFilename());
                media.setType(MediaType.IMAGE);
                media.setService(service);
                if (isFirstImage) {
                    media.setPrimary(true);
                    isFirstImage = false;
                }
                mediaRepository.save(media);
            }
        }
    }
}