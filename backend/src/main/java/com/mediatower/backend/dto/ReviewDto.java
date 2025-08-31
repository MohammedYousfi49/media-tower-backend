package com.mediatower.backend.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ReviewDto {
    private Long id;
    @NotNull private Long productId;
    private Long userId;
    private String userName;
    @NotNull @Min(1) @Max(5) private Integer rating;
    private String comment;
    private LocalDateTime reviewDate;
}