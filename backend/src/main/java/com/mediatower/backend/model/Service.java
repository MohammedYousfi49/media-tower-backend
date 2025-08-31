package com.mediatower.backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "services")
@Data
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_names", joinColumns = @JoinColumn(name = "service_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "name", nullable = false)
    private Map<String, String> names = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_descriptions", joinColumns = @JoinColumn(name = "service_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "description", columnDefinition = "TEXT")
    private Map<String, String> descriptions = new HashMap<>();

    @Column(nullable = false)
    private BigDecimal price;

    // --- SUPPRESSION DE L'ANCIEN CHAMP ---
    // private String imageUrl;

    // --- NOUVELLE RELATION (pour les images de présentation uniquement) ---
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("service-media")
    private List<Media> mediaAssets = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}