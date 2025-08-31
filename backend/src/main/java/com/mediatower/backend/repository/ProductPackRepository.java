package com.mediatower.backend.repository;

import com.mediatower.backend.model.ProductPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPackRepository extends JpaRepository<ProductPack, Long> {
}