package com.mediatower.backend.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
@Data
public class ServiceDto {
    private Long id;
    private Map<String, String> names, descriptions;
    private BigDecimal price;
    private List<MediaDto> images;
    private Long bookingCount;

}