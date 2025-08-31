package com.mediatower.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Setting {

    @Id
    @Column(name = "setting_key", length = 50, nullable = false)
    private String key; // Ex: "logoUrl", "facebookUrl"

    @Column(name = "setting_value", length = 2000)
    private String value; // Ex: "https://example.com/logo.png"
}