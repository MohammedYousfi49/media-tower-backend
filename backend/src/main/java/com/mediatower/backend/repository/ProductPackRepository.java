package com.mediatower.backend.repository;

import com.mediatower.backend.model.ProductPack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPackRepository extends JpaRepository<ProductPack, Long> {
    @Query("SELECT DISTINCT p FROM ProductPack p LEFT JOIN p.names n WHERE " +
            "(:searchTerm IS NULL OR (KEY(n) IN ('en', 'fr') AND LOWER(VALUE(n)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))))")
    Page<ProductPack> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
}