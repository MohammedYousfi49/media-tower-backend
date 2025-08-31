package com.mediatower.backend.controller;

import com.mediatower.backend.model.Media;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.MediaRepository;
import com.mediatower.backend.repository.UserProductAccessRepository;
import com.mediatower.backend.repository.UserRepository;
import com.mediatower.backend.service.FileStorageService;
import com.mediatower.backend.model.MediaType;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils; // <-- AJOUTER CET IMPORT

import java.io.IOException;
import java.nio.charset.StandardCharsets; // <-- AJOUTER CET IMPORT
import java.nio.file.Files;

@RestController
@RequestMapping("/api/download")
public class FileDownloadController {

    private final FileStorageService fileStorageService;
    private final MediaRepository mediaRepository;
    private final UserProductAccessRepository userProductAccessRepository;
    private final UserRepository userRepository;

    public FileDownloadController(FileStorageService fileStorageService, MediaRepository mediaRepository, UserProductAccessRepository userProductAccessRepository, UserRepository userRepository) {
        this.fileStorageService = fileStorageService;
        this.mediaRepository = mediaRepository;
        this.userRepository = userRepository;
        this.userProductAccessRepository = userProductAccessRepository; // Initialiser ici aussi
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, Authentication authentication) {

        Media media = mediaRepository.findByFileName(fileName)
                .orElseThrow(() -> new RuntimeException("File not found with name " + fileName));

        Resource resource = fileStorageService.loadFileAsResource(media.getFileName());

        String contentType = null;
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ex) {
            System.err.println("Could not determine file type for " + fileName + ": " + ex.getMessage());
        }
        if (contentType == null) {
            contentType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        // --- DÉBUT DE LA CORRECTION : GESTION DU NOM DE FICHIER POUR L'EN-TÊTE ---
        String filenameToUseInHeader;
        try {
            // Encoder le nom de fichier original pour s'assurer qu'il est compatible avec les en-têtes HTTP
            // Cela gère les caractères non-ASCII en les transformant en séquence encodée (%xx)
            filenameToUseInHeader = UriUtils.encode(media.getOriginalName(), StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            // En cas d'échec d'encodage (très rare), utiliser un nom de fichier plus sûr (le UUID généré)
            System.err.println("Failed to encode original filename for header: " + media.getOriginalName() + ". Using generated filename instead. Error: " + e.getMessage());
            filenameToUseInHeader = UriUtils.encode(media.getFileName(), StandardCharsets.UTF_8.toString());
        }
        // --- FIN DE LA CORRECTION ---


        if (media.getType() == MediaType.DIGITAL_ASSET) {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            User currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

            // Assurez-vous que media.getProduct() n'est pas nul si un Digital Asset est toujours lié à un produit.
            // Ajouter une vérification pour éviter NullPointerException
            Long productId = (media.getProduct() != null) ? media.getProduct().getId() : null;

            // Si le produitId est nul pour un DIGITAL_ASSET, on pourrait refuser l'accès ou logger une erreur.
            // Pour l'instant, je vais considérer que le digital asset devrait avoir un produit associé.
            if (productId == null) {
                System.err.println("Digital asset " + media.getFileName() + " is not linked to any product. Access denied.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }


            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));
            boolean hasAccess = userProductAccessRepository.existsByUserIdAndProductId(currentUser.getId(), productId);

            if (!isAdmin && !hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filenameToUseInHeader + "\"") // <-- UTILISER filenameToUseInHeader
                    .body(resource);

        } else if (media.getType() == MediaType.IMAGE) {
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filenameToUseInHeader + "\"") // <-- UTILISER filenameToUseInHeader
                    .body(resource);
        } else {
            System.err.println("Attempted to access unsupported media type: " + media.getType());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}