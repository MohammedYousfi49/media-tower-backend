package com.mediatower.backend.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private Long id;

    @NotNull(message = "Product ID is required for order item")
    private Long productId;  // doit être Long

    private String productName; // optionnel, pour affichage

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}