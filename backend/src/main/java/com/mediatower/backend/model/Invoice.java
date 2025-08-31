package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber; // Ex: INV-2025-0001

    @OneToOne(fetch = FetchType.LAZY) // Une facture est liée à une commande unique
    @JoinColumn(name = "order_id", unique = true, nullable = false)
    private Order order;

    @Column(nullable = false)
    private LocalDateTime invoiceDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalHT; // Total Hors Taxes

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount; // Montant de la TVA

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalTTC; // Total Toutes Taxes Comprises

    @Column(nullable = false)
    private Boolean includesVAT; // Indique si la TVA est appliquée ou non

    @Column(length = 2000)
    private String billingAddress; // Adresse de facturation

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.invoiceDate = LocalDateTime.now(); // Date de la facture
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}