package com.mediatower.backend.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ServiceReviewDto {
    private Long id, serviceId, userId;
    private String userName, comment;
    private Integer rating;
    private LocalDateTime reviewDate;
}