package com.mediatower.backend.repository;

import com.mediatower.backend.model.Category;
import com.mediatower.backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    long countByStockLessThanEqual(Integer stock);
    long countByCategoryId(Long categoryId);
    @Query("SELECT DISTINCT p FROM Product p JOIN p.category c LEFT JOIN p.names n WHERE " +
            "(:searchTerm IS NULL OR (KEY(n) IN ('en', 'fr') AND LOWER(VALUE(n)) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) AND " +
            "(:categoryId IS NULL OR c.id = :categoryId) AND " +
            "(:stockStatus IS NULL OR " +
            " (:stockStatus = 'instock' AND p.stock > 10) OR " +
            " (:stockStatus = 'lowstock' AND p.stock > 0 AND p.stock <= 10) OR " +
            " (:stockStatus = 'outofstock' AND p.stock = 0))")
    Page<Product> findWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("categoryId") Long categoryId,
            @Param("stockStatus") String stockStatus,
            Pageable pageable
    );



}