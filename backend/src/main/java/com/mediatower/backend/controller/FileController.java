package com.mediatower.backend.controller;

import com.mediatower.backend.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/generate-upload-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> generateUploadUrl(@RequestBody Map<String, String> payload) {
        try {
            String originalFileName = payload.get("fileName");
            String contentType = payload.get("contentType");

            if (originalFileName == null || contentType == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "fileName and contentType are required."));
            }

            String uniqueFileName = "chat-attachments/" + UUID.randomUUID() + "-" + originalFileName;

            URL signedUrl = fileStorageService.generateSignedUploadUrlForChat(uniqueFileName, contentType);

            String publicUrl = fileStorageService.getPublicUrlForChat(uniqueFileName);

            return ResponseEntity.ok(Map.of("uploadUrl", signedUrl.toString(), "publicUrl", publicUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}