package com.mediatower.backend.repository;

import com.mediatower.backend.model.Category;
import com.mediatower.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Exemple de méthode personnalisée pour trouver des produits par nom (ignorando la casse)
   // List<Product> findByNameContainingIgnoreCase(String name);

    // Exemple pour trouver des produits par catégorie
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findByIdIn(List<Long> ids);
    List<Product> findTop4ByCategoryAndIdNot(Category category, Long id);

    // List<Product> findByNameContainingIgnoreCaseAndCategoryId(String name, Long categoryId);
}