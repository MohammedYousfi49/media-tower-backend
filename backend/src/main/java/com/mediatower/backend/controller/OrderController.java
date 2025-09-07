package com.mediatower.backend.controller;

import com.mediatower.backend.dto.OrderDto;
import com.mediatower.backend.model.OrderStatus;
import com.mediatower.backend.model.UserRole;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDto>> getAllOrders(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<OrderDto> orders = orderService.getAllOrdersPaginated(search, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<OrderDto> getMyOrders(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        return orderService.getOrdersByUserId(firebaseUser.getUid());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id, @AuthenticationPrincipal FirebaseUser firebaseUser) {
        try {
            OrderDto order = orderService.getOrderById(id);

            boolean isAdmin = firebaseUser.getRole().equals(UserRole.ADMIN);
            // On vérifie que le champ 'user' n'est pas null avant d'appeler getUid()
            boolean isOwner = order.getUser() != null && order.getUser().getUid().equals(firebaseUser.getUid());

            if (isAdmin || isOwner) {
                return ResponseEntity.ok(order);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createOrder(@AuthenticationPrincipal FirebaseUser firebaseUser, @RequestBody OrderDto orderDto) {
        try {
            OrderDto createdOrder = orderService.createOrder(firebaseUser.getUid(), orderDto);
            return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String statusStr = payload.get("status");
            if (statusStr == null || statusStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Status is required in payload.");
            }
            OrderStatus status = OrderStatus.valueOf(statusStr.toUpperCase());
            OrderDto updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid status value.", HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/can-review/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> canUserReview(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long productId) {
        boolean canReview = orderService.canUserReviewProduct(firebaseUser.getUid(), productId);
        return ResponseEntity.ok(Collections.singletonMap("canReview", canReview));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}