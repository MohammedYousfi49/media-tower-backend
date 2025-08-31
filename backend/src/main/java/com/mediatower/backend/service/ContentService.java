package com.mediatower.backend.service;

import com.mediatower.backend.dto.ContentDto;
import com.mediatower.backend.model.Content;
import com.mediatower.backend.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContentService {
    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    public List<ContentDto> getAllContent() {
        return contentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ContentDto> getContentBySlug(String slug) {
        return contentRepository.findBySlug(slug).map(this::convertToDto);
    }

    public ContentDto createContent(ContentDto contentDto) {
        if (contentRepository.findBySlug(contentDto.getSlug()).isPresent()) {
            throw new IllegalArgumentException("Content with this slug already exists.");
        }
        Content content = convertToEntity(contentDto);
        return convertToDto(contentRepository.save(content));
    }

    public ContentDto updateContent(Long id, ContentDto contentDto) {
        Content existingContent = contentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Content not found with ID: " + id));

        if (!existingContent.getSlug().equals(contentDto.getSlug()) &&
                contentRepository.findBySlug(contentDto.getSlug()).isPresent()) {
            throw new IllegalArgumentException("Content with this slug already exists.");
        }

        existingContent.setSlug(contentDto.getSlug());
        existingContent.setTitles(contentDto.getTitles());
        existingContent.setBodies(contentDto.getBodies());
        return convertToDto(contentRepository.save(existingContent));
    }

    public void deleteContent(Long id) {
        if (!contentRepository.existsById(id)) {
            throw new RuntimeException("Content not found with ID: " + id);
        }
        contentRepository.deleteById(id);
    }

    private ContentDto convertToDto(Content content) {
        return new ContentDto(
                content.getId(),
                content.getSlug(),
                content.getTitles(),
                content.getBodies(),
                content.getCreatedAt(),
                content.getUpdatedAt()
        );
    }

    private Content convertToEntity(ContentDto contentDto) {
        Content content = new Content();
        content.setId(contentDto.getId());
        content.setSlug(contentDto.getSlug());
        content.setTitles(contentDto.getTitles());
        content.setBodies(contentDto.getBodies());
        return content;
    }
}
