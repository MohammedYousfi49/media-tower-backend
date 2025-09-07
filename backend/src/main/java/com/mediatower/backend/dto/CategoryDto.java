// src/main/java/com/mediatower/backend/dto/CategoryDto.java

package com.mediatower.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;

    @NotEmpty(message = "Category names cannot be empty")
    private Map<String, String> names;

    private Map<String, String> descriptions;
    private Long productCount;

}