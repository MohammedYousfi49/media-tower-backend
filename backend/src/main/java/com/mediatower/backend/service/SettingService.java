// src/main/java/com/mediatower/backend/service/SettingService.java
package com.mediatower.backend.service;

import com.mediatower.backend.dto.SettingDto;
import com.mediatower.backend.model.Setting;
import com.mediatower.backend.repository.SettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap; // <-- NOUVEL IMPORT
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    // Récupérer tous les paramètres
    public List<SettingDto> getAllSettings() {
        return settingRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @PostConstruct
    @Transactional
    public void initializeDefaultSettings() {
        // ==================== CORRECTION : Utilisation d'une méthode plus flexible ====================
        // On crée une Map mutable au lieu d'utiliser Map.of() qui est limité à 10 paires.
        Map<String, String> defaultSettings = new HashMap<>();
        defaultSettings.put("site_title", "Media Tower");
        defaultSettings.put("site_tagline", "Your Digital Content Marketplace");
        defaultSettings.put("site_logo_url", "/default-logo.png");
        defaultSettings.put("site_color_primary", "#a855f7");
        defaultSettings.put("contact_email", "contact@example.com");
        defaultSettings.put("social_facebook_url", "");
        defaultSettings.put("social_twitter_url", "");
        defaultSettings.put("social_instagram_url", "");
        defaultSettings.put("maintenance_mode_enabled", "false");
        defaultSettings.put("maintenance_mode_message", "Our site is currently down for maintenance. We'll be back shortly!");
        defaultSettings.put("seo_home_title", "Media Tower - Home");
        defaultSettings.put("seo_home_description", "Discover amazing digital products and services.");
        // ============================================================================================

        defaultSettings.forEach((key, value) -> {
            if (!settingRepository.existsById(key)) {
                settingRepository.save(new Setting(key, value));
            }
        });
    }

    // Mettre à jour une liste de paramètres
    @Transactional
    public List<SettingDto> updateSettings(List<SettingDto> settingsToUpdate) {
        List<Setting> updatedSettings = settingsToUpdate.stream()
                .map(dto -> new Setting(dto.getKey(), dto.getValue()))
                .collect(Collectors.toList());

        settingRepository.saveAll(updatedSettings);

        return settingsToUpdate;
    }

    private SettingDto convertToDto(Setting setting) {
        return new SettingDto(setting.getKey(), setting.getValue());
    }
}