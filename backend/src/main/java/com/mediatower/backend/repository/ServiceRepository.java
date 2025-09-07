package com.mediatower.backend.repository;

import com.mediatower.backend.model.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findTop4ByIdNot(Long id);
    @Query("SELECT DISTINCT s FROM Service s LEFT JOIN s.names n WHERE " +
            "(:searchTerm IS NULL OR (KEY(n) IN ('en', 'fr') AND LOWER(VALUE(n)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))))")
    Page<Service> findBySearchTerm(@Param("searchTerm") String searchTerm, Pageable pageable);

}