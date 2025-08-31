// src/main/java/com/mediatower/backend/model/Content.java

package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "content")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "content_titles", joinColumns = @JoinColumn(name = "content_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "title", nullable = false)
    private Map<String, String> titles = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "content_bodies", joinColumns = @JoinColumn(name = "content_id"))
    @MapKeyColumn(name = "language_code", length = 10)
    @Column(name = "body", columnDefinition = "TEXT")
    private Map<String, String> bodies = new HashMap<>();

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