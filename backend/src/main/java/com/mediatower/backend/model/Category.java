// src/main/java/com/mediatower/backend/model/Category.java

package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_names", joinColumns = @JoinColumn(name = "category_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "name", nullable = false, length = 100)
    private Map<String, String> names = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "category_descriptions", joinColumns = @JoinColumn(name = "category_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "description", length = 500)
    private Map<String, String> descriptions = new HashMap<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}