// src/main/java/com/mediatower/backend/service/CategoryService.java

package com.mediatower.backend.service;

import com.mediatower.backend.dto.CategoryDto;
import com.mediatower.backend.model.Category;
import com.mediatower.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<CategoryDto> getCategoryById(Long id) {
        return categoryRepository.findById(id).map(this::convertToDto);
    }

    public CategoryDto createCategory(CategoryDto categoryDto) {
        // La vérification d'unicité est plus complexe maintenant, on peut la skipper ou vérifier une langue par défaut
        Category category = convertToEntity(categoryDto);
        return convertToDto(categoryRepository.save(category));
    }

    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + id));

        existingCategory.setNames(categoryDto.getNames());
        existingCategory.setDescriptions(categoryDto.getDescriptions());
        return convertToDto(categoryRepository.save(existingCategory));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with ID: " + id);
        }
        categoryRepository.deleteById(id);
    }

    // --- Conversion Utilities ---
    public CategoryDto convertToDto(Category category) {
        return new CategoryDto(category.getId(), category.getNames(), category.getDescriptions());
    }

    public Category convertToEntity(CategoryDto categoryDto) {
        Category category = new Category();
        category.setId(categoryDto.getId());
        category.setNames(categoryDto.getNames());
        category.setDescriptions(categoryDto.getDescriptions());
        return category;
    }
}