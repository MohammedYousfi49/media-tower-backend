// Fichier : src/main/java/com/mediatower/backend/service/ServiceService.java (COMPLET ET CORRIGÉ)

package com.mediatower.backend.service;

import com.mediatower.backend.dto.MediaDto;
import com.mediatower.backend.dto.ServiceDto;
import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.MediaType;
import com.mediatower.backend.model.Service;
import com.mediatower.backend.repository.BookingRepository;
import com.mediatower.backend.repository.MediaRepository;
import com.mediatower.backend.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final FileStorageService fileStorageService;
    private final MediaRepository mediaRepository;

    private final String baseUrl = "http://localhost:8080/api/download/";

    public ServiceService(ServiceRepository serviceRepository, BookingRepository bookingRepository, FileStorageService fileStorageService, MediaRepository mediaRepository) {
        this.serviceRepository = serviceRepository;
        this.bookingRepository = bookingRepository;
        this.fileStorageService = fileStorageService;
        this.mediaRepository = mediaRepository;
    }

    public Page<ServiceDto> getAllServicesPaginated(String searchTerm, Pageable pageable) {
        String search = (searchTerm == null || searchTerm.trim().isEmpty()) ? null : searchTerm;
        Page<Service> servicePage = serviceRepository.findBySearchTerm(search, pageable);
        return servicePage.map(this::convertToDto);
    }

    public Optional<ServiceDto> getServiceById(Long id) {
        return serviceRepository.findById(id).map(this::convertToDto);
    }

    // CORRECTION: Méthode pour admin - sélection simplifiée
    public List<ServiceDto> getAllServicesForSelection() {
        return serviceRepository.findAll().stream()
                .map(service -> {
                    ServiceDto dto = new ServiceDto();
                    dto.setId(service.getId());
                    dto.setNames(service.getNames());
                    dto.setPrice(service.getPrice());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceDto createService(ServiceDto serviceDto, List<MultipartFile> images) {
        Service service = new Service();
        updateServiceFromDto(service, serviceDto);

        Service savedService = serviceRepository.save(service);

        associateMedia(savedService, images);

        return convertToDto(savedService);
    }

    @Transactional
    public ServiceDto updateService(Long id, ServiceDto serviceDto, List<MultipartFile> images) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        updateServiceFromDto(service, serviceDto);

        associateMedia(service, images);

        return convertToDto(serviceRepository.save(service));
    }

    private void associateMedia(Service service, List<MultipartFile> images) {
        if (images != null && !images.isEmpty()) {
            boolean isFirstImage = service.getMediaAssets().stream().noneMatch(Media::isPrimary);
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
                service.getMediaAssets().add(media);
            }
        }
    }

    private void updateServiceFromDto(Service service, ServiceDto dto) {
        service.setNames(dto.getNames());
        service.setDescriptions(dto.getDescriptions());
        service.setPrice(dto.getPrice());
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    public List<ServiceDto> getSimilarServices(Long serviceId) {
        List<Service> similarServices = serviceRepository.findTop4ByIdNot(serviceId);
        return similarServices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ServiceDto convertToDto(Service service) {
        ServiceDto dto = new ServiceDto();
        dto.setId(service.getId());
        dto.setNames(service.getNames());
        dto.setDescriptions(service.getDescriptions());
        dto.setPrice(service.getPrice());
        dto.setBookingCount(bookingRepository.countByServiceId(service.getId()));

        if (service.getMediaAssets() != null) {
            dto.setImages(service.getMediaAssets().stream()
                    .filter(media -> media.getType() == MediaType.IMAGE)
                    .map(this::convertMediaToDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private MediaDto convertMediaToDto(Media media) {
        MediaDto mediaDto = new MediaDto();
        mediaDto.setId(media.getId());
        mediaDto.setFileName(media.getFileName());
        mediaDto.setOriginalName(media.getOriginalName());
        mediaDto.setType(media.getType().name());
        mediaDto.setUrl(baseUrl + media.getFileName());
        mediaDto.setPrimary(media.isPrimary());
        return mediaDto;
    }
}