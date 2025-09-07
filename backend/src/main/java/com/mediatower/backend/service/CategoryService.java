package com.mediatower.backend.service;

import com.mediatower.backend.dto.CategoryDto;
import com.mediatower.backend.model.Category;
import com.mediatower.backend.repository.CategoryRepository;
import com.mediatower.backend.repository.ProductRepository; // <-- AJOUT DE L'IMPORT
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository; // <-- AJOUT DE LA DÉPENDANCE

    // --- MISE À JOUR DU CONSTRUCTEUR ---
    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public Page<CategoryDto> getAllCategoriesPaginated(String search, Pageable pageable) {
        Page<Category> categoryPage;
        if (search != null && !search.trim().isEmpty()) {
            categoryPage = categoryRepository.findByNamesContainingIgnoreCaseWithCollections(search, pageable);
        } else {
            categoryPage = categoryRepository.findAllWithCollections(pageable);
        }
        return categoryPage.map(this::convertToDto);

    }

    public Optional<CategoryDto> getCategoryById(Long id) {
        return categoryRepository.findById(id).map(this::convertToDto);
    }

    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category category = convertToEntity(categoryDto);
        Category savedCategory = categoryRepository.save(category);
        return convertToDto(savedCategory); // On reconvertit pour avoir un DTO complet avec l'ID et le productCount
    }

    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + id));

        existingCategory.setNames(categoryDto.getNames());
        existingCategory.setDescriptions(categoryDto.getDescriptions());
        Category updatedCategory = categoryRepository.save(existingCategory);
        return convertToDto(updatedCategory);
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with ID: " + id);
        }
        // --- AJOUT D'UNE VÉRIFICATION DE SÉCURITÉ ---
        // Empêche la suppression d'une catégorie non vide.
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category with ID: " + id + " because it contains " + productCount + " products.");
        }
        categoryRepository.deleteById(id);
    }

    // --- MISE À JOUR DE LA MÉTHODE DE CONVERSION ---
    public CategoryDto convertToDto(Category category) {
        // Pour chaque catégorie, on compte le nombre de produits associés
        long count = productRepository.countByCategoryId(category.getId());
        return new CategoryDto(
                category.getId(),
                category.getNames(),
                category.getDescriptions(),
                count // On ajoute le compte au DTO
        );
    }

    public Category convertToEntity(CategoryDto categoryDto) {
        Category category = new Category();
        category.setId(categoryDto.getId());
        category.setNames(categoryDto.getNames());
        category.setDescriptions(categoryDto.getDescriptions());
        return category;
    }
    public List<CategoryDto> getAllCategoriesList() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}