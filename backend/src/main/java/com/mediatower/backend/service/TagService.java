package com.mediatower.backend.service;
import com.mediatower.backend.dto.TagDto;
import com.mediatower.backend.model.Tag;
import com.mediatower.backend.repository.TagRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {
    private final TagRepository tagRepository;
    public TagService(TagRepository tagRepository) { this.tagRepository = tagRepository; }

    public List<TagDto> getAllTags() {
        return tagRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public TagDto createTag(TagDto dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());
        return convertToDto(tagRepository.save(tag));
    }

    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
    }

    private TagDto convertToDto(Tag tag) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        return dto;
    }
}