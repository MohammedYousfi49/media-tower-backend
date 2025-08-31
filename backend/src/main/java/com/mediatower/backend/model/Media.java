package com.mediatower.backend.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "media")
@Data
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName; // Nom unique sur le serveur (ex: uuid-monfichier.pdf)

    @Column(nullable = false)
    private String originalName; // Nom original du fichier (ex: mon-ebook.pdf)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type; // IMAGE ou DIGITAL_ASSET

    // Relation avec Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonBackReference("product-media")
    private Product product;

    // Relation avec Service (pour les images de présentation des services)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    @JsonBackReference("service-media")
    private Service service;
    @Column(nullable = false)
    private boolean isPrimary = false;

    // --- LIAISON MANQUANTE À AJOUTER ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id")
    @JsonBackReference("pack-media")
    private ProductPack pack;
}