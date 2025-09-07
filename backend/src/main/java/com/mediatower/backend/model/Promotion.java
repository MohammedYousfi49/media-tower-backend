// Fichier : src/main/java/com/mediatower/backend/model/Promotion.java (COMPLET ET MIS À JOUR)

package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "promotions")
@Data
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private BigDecimal discountValue;

    private LocalDateTime startDate, endDate;

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "promotion_products", joinColumns = @JoinColumn(name = "promotion_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> applicableProducts = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "promotion_services", joinColumns = @JoinColumn(name = "promotion_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private Set<Service> applicableServices = new HashSet<>();

    // --- DÉBUT DE L'AJOUT ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "promotion_packs", joinColumns = @JoinColumn(name = "promotion_id"), inverseJoinColumns = @JoinColumn(name = "pack_id"))
    private Set<ProductPack> applicablePacks = new HashSet<>();
    // --- FIN DE L'AJOUT ---
}