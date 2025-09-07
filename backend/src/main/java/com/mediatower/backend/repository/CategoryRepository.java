package com.mediatower.backend.repository;

import com.mediatower.backend.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    //Optional<Category> findByName(String name);

    // Requête pour la recherche paginée, qui charge aussi les collections
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.names LEFT JOIN FETCH c.descriptions WHERE " +
            "EXISTS (SELECT 1 FROM c.names n WHERE LOWER(n) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Category> findByNamesContainingIgnoreCaseWithCollections(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Requête pour la pagination simple, qui charge aussi les collections
    @Query(value = "SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.names LEFT JOIN FETCH c.descriptions",
            countQuery = "SELECT COUNT(c) FROM Category c")
    Page<Category> findAllWithCollections(Pageable pageable);
}