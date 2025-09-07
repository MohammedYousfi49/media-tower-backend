// Fichier : src/main/java/com/mediatower/backend/controller/ServiceController.java (COMPLET ET CORRIGÉ)

package com.mediatower.backend.controller;
import com.mediatower.backend.dto.ServiceDto;
import com.mediatower.backend.service.ServiceService;
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
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @GetMapping
    public ResponseEntity<Page<ServiceDto>> getAllServices(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<ServiceDto> services = serviceService.getAllServicesPaginated(search, pageable);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable Long id) {
        return serviceService.getServiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CORRECTION: Endpoint pour admin uniquement
    @GetMapping("/all")
    public ResponseEntity<List<ServiceDto>> getAllServicesForAdmin() {
        return ResponseEntity.ok(serviceService.getAllServicesForSelection());
    }

    @PostMapping(consumes = { "multipart/form-data" })
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDto> createService(
            @Valid @RequestPart("serviceDto") ServiceDto serviceDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images) {

        ServiceDto createdServiceDto = serviceService.createService(serviceDto, images);
        return new ResponseEntity<>(createdServiceDto, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceDto> updateService(
            @PathVariable Long id,
            @Valid @RequestPart("serviceDto") ServiceDto serviceDto,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> images) {

        ServiceDto updatedDto = serviceService.updateService(id, serviceDto, images);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<ServiceDto>> getSimilarServices(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getSimilarServices(id));
    }
}