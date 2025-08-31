// src/main/java/com/mediatower/backend/dto/ContentDto.java

package com.mediatower.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentDto {
    private Long id;
    private String slug;
    private Map<String, String> titles;
    private Map<String, String> bodies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}