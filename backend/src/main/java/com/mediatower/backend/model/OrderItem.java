// Fichier : src/main/java/com/mediatower/backend/model/OrderItem.java (CORRIGÉ ET COMPLET)

package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // --- DÉBUT DES MODIFICATIONS ---
    // Le produit devient optionnel
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // On ajoute les liens vers Service et ProductPack
    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne
    @JoinColumn(name = "pack_id")
    private ProductPack pack;
    // --- FIN DES MODIFICATIONS ---

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;
}