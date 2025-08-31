package com.mediatower.backend.service;

import com.mediatower.backend.dto.SettingDto;
import com.mediatower.backend.model.Setting;
import com.mediatower.backend.repository.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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