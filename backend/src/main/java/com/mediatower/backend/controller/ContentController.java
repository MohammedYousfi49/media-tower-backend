package com.mediatower.backend.controller;

import com.mediatower.backend.dto.ContentDto;
import com.mediatower.backend.service.ContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/content")
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    public List<ContentDto> getAllContent() {
        return contentService.getAllContent();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ContentDto> getContentBySlug(@PathVariable String slug) {
        Optional<ContentDto> content = contentService.getContentBySlug(slug);
        return content.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // MODIFIÉ ICI
    public ResponseEntity<?> createContent(@RequestBody ContentDto contentDto) {
        try {
            ContentDto createdContent = contentService.createContent(contentDto);
            return new ResponseEntity<>(createdContent, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // MODIFIÉ ICI
    public ResponseEntity<?> updateContent(@PathVariable Long id, @RequestBody ContentDto contentDto) {
        try {
            ContentDto updatedContent = contentService.updateContent(id, contentDto);
            return ResponseEntity.ok(updatedContent);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // MODIFIÉ ICI
    public ResponseEntity<Void> deleteContent(@PathVariable Long id) {
        try {
            contentService.deleteContent(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}