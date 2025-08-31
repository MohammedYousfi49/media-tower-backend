// Fichier : src/main/java/com/mediatower/backend/service/ServiceService.java (COMPLET ET FINAL)

package com.mediatower.backend.service;

import com.mediatower.backend.dto.MediaDto;
import com.mediatower.backend.dto.ServiceDto;
import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.MediaType;
import com.mediatower.backend.model.Service;
import com.mediatower.backend.repository.ServiceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final String baseUrl = "http://localhost:8080/api/download/";

    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<ServiceDto> getAllServices() {
        return serviceRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public Optional<ServiceDto> getServiceById(Long id) {
        return serviceRepository.findById(id).map(this::convertToDto);
    }

    @Transactional
    public ServiceDto createService(ServiceDto serviceDto) {
        Service service = new Service();
        updateServiceFromDto(service, serviceDto);
        return convertToDto(serviceRepository.save(service));
    }

    @Transactional
    public ServiceDto updateService(Long id, ServiceDto serviceDto) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        updateServiceFromDto(service, serviceDto);
        return convertToDto(serviceRepository.save(service));
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