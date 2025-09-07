package com.mediatower.backend.controller;

import com.mediatower.backend.dto.CategoryDto;
import com.mediatower.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

//    @GetMapping
//    public List<CategoryDto> getAllCategories() {
//        return categoryService.getAllCategories();
@GetMapping("/all")
public List<CategoryDto> getAllCategoriesList() {
    return categoryService.getAllCategoriesList();
}
//    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        Optional<CategoryDto> category = categoryService.getCategoryById(id);
        return category.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PostMapping
    // Ligne modifiée : Utilise hasRole('ADMIN') au lieu de hasAuthority('ADMIN')
    @PreAuthorize("hasRole('ADMIN')") // Seuls les admins peuvent créer des catégories
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        try {
            CategoryDto createdCategory = categoryService.createCategory(categoryDto);
            return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT); // 409 Conflict si nom déjà utilisé
        }
    }

    @PutMapping("/{id}")
    // Ligne modifiée : Utilise hasRole('ADMIN') au lieu de hasAuthority('ADMIN')
    @PreAuthorize("hasRole('ADMIN')") // Seuls les admins peuvent modifier des catégories
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDto categoryDto) {
        try {
            CategoryDto updatedCategory = categoryService.updateCategory(id, categoryDto);
            return ResponseEntity.ok(updatedCategory);
        } catch (IllegalArgumentException e) { // Nom déjà existant - doit être attrapé AVANT RuntimeException
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) { // Catégorie non trouvée ou autre erreur d'exécution générique
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    // Ligne modifiée : Utilise hasRole('ADMIN') au lieu de hasAuthority('ADMIN')
    @PreAuthorize("hasRole('ADMIN')") // Seuls les admins peuvent supprimer des catégories
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping
    public ResponseEntity<Page<CategoryDto>> getAllCategories(
            @RequestParam(required = false, defaultValue = "") String search,
            Pageable pageable) {
        Page<CategoryDto> categories = categoryService.getAllCategoriesPaginated(search, pageable);
        return ResponseEntity.ok(categories);
    }
}