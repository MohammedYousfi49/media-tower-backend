package com.mediatower.backend.dto;

import lombok.Data;

@Data
public class MediaDto {
    private Long id;
    private String fileName;
    private String originalName;
    private String type;
    private String url; // URL complète pour l'accès
    private boolean isPrimary;
}