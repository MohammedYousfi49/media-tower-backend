// Chemin : src/main/java/com/mediatower/backend/model/ProductPack.java
package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "product_packs")
// =========== CORRECTION : Remplacer @Data et ajouter des exclusions ===========
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"products", "mediaAssets"}) // Exclure les collections pour éviter les boucles
@EqualsAndHashCode(of = "id") // Baser l'égalité uniquement sur l'ID
public class ProductPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pack_names", joinColumns = @JoinColumn(name = "pack_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "name", nullable = false)
    private Map<String, String> names = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pack_descriptions", joinColumns = @JoinColumn(name = "pack_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "description", columnDefinition = "TEXT")
    private Map<String, String> descriptions = new HashMap<>();

    @Column(nullable = false)
    private BigDecimal price;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id")
    private List<Media> mediaAssets = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "pack_items",
            joinColumns = @JoinColumn(name = "pack_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();
}