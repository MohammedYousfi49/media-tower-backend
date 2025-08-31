package com.mediatower.backend.repository;

import com.mediatower.backend.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeAndIsActiveTrue(String code);
    List<Promotion> findByApplicableProducts_IdAndCodeIsNullAndIsActiveTrueAndStartDateBeforeAndEndDateAfter(Long productId, LocalDateTime now, LocalDateTime nowAgain);
    List<Promotion> findByApplicableServices_IdAndCodeIsNullAndIsActiveTrueAndStartDateBeforeAndEndDateAfter(Long serviceId, LocalDateTime now, LocalDateTime nowAgain);
}