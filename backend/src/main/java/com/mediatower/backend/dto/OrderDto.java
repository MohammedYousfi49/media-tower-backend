package com.mediatower.backend.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.mediatower.backend.dto.OrderItemDto;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long id;
    private UserInfo user;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemDto> orderItems;
    private String paymentMethod;
    private String promotionCode;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String uid;
        private String firstName;
        private String lastName;
        private String email;
    }
}