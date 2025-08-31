// Fichier : src/main/java/com/mediatower/backend/service/MediaService.java (NOUVEAU FICHIER)

package com.mediatower.backend.service;

import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.Product;
import com.mediatower.backend.model.ProductPack;
import com.mediatower.backend.model.Service;
import com.mediatower.backend.repository.MediaRepository;
import com.mediatower.backend.repository.ProductPackRepository;
import com.mediatower.backend.repository.ProductRepository;
import com.mediatower.backend.repository.ServiceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class MediaService {

    private final MediaRepository mediaRepository;
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final ProductPackRepository packRepository;

    public MediaService(MediaRepository mediaRepository, ProductRepository productRepository, ServiceRepository serviceRepository, ProductPackRepository packRepository) {
        this.mediaRepository = mediaRepository;
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.packRepository = packRepository;
    }

    public void deleteMedia(Long mediaId) {
        // La suppression est gérée par la cascade, mais on peut ajouter une logique de suppression de fichier ici si besoin
        mediaRepository.deleteById(mediaId);
    }

    @Transactional
    public void setPrimaryMediaForProduct(Long productId, Long mediaIdToSetAsPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        setPrimary(product.getMediaAssets(), mediaIdToSetAsPrimary);
    }

    @Transactional
    public void setPrimaryMediaForService(Long serviceId, Long mediaIdToSetAsPrimary) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        setPrimary(service.getMediaAssets(), mediaIdToSetAsPrimary);
    }

    @Transactional
    public void setPrimaryMediaForPack(Long packId, Long mediaIdToSetAsPrimary) {
        ProductPack pack = packRepository.findById(packId)
                .orElseThrow(() -> new RuntimeException("Pack not found"));
        setPrimary(pack.getMediaAssets(), mediaIdToSetAsPrimary);
    }

    // Méthode générique pour définir une image principale dans une liste
    private void setPrimary(List<Media> mediaList, Long mediaIdToSetAsPrimary) {
        boolean mediaFound = false;
        for (Media media : mediaList) {
            if (media.getId().equals(mediaIdToSetAsPrimary)) {
                media.setPrimary(true);
                mediaFound = true;
            } else {
                media.setPrimary(false);
            }
        }
        if (!mediaFound) {
            throw new RuntimeException("Media ID " + mediaIdToSetAsPrimary + " not found in the list.");
        }
        mediaRepository.saveAll(mediaList);
    }
}