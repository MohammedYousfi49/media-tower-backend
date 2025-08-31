package com.mediatower.backend.controller;

import com.mediatower.backend.dto.SettingDto;
import com.mediatower.backend.service.SettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    // Note: Les paramètres peuvent être publics pour que le frontend les utilise
    public ResponseEntity<List<SettingDto>> getAllSettings() {
        return ResponseEntity.ok(settingService.getAllSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')") // MODIFIÉ ICI : hasAuthority('ADMIN') -> hasRole('ADMIN')
    public ResponseEntity<List<SettingDto>> updateSettings(@RequestBody List<SettingDto> settings) {
        return ResponseEntity.ok(settingService.updateSettings(settings));
    }
}