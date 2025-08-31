package com.mediatower.backend.controller;

import com.mediatower.backend.dto.AdminUserDto;
import com.mediatower.backend.dto.OrderDto;
import com.mediatower.backend.dto.ReviewDto;
import com.mediatower.backend.dto.UserProfileDto;
import com.mediatower.backend.model.User;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.OrderService;
import com.mediatower.backend.service.ReviewService;
import com.mediatower.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public UserController(UserService userService, OrderService orderService, ReviewService reviewService) {
        this.userService = userService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAuthenticatedUserProfile(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        Optional<User> userOptional = userService.findUserByEmail(firebaseUser.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "USER_NOT_FOUND_IN_DB", "message", "User not found in application database."));
        }

        User user = userOptional.get();
        return ResponseEntity.ok(userService.convertToDto(user));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileDto> updateAuthenticatedUserProfile(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @RequestBody UserProfileDto userProfileDto) {
        return userService.findUserByEmail(firebaseUser.getEmail())
                .map(user -> userService.updateUserProfile(user.getId(), userProfileDto))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/me/orders")
    @PreAuthorize("isAuthenticated()")
    public List<OrderDto> getUserOrders(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        return orderService.getOrdersByUserId(firebaseUser.getUid());
    }

    @GetMapping("/me/reviews")
    @PreAuthorize("isAuthenticated()")
    public List<ReviewDto> getUserReviews(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        return reviewService.getReviewsByUserId(firebaseUser.getUid());
    }

    @PutMapping("/me/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileDto> updateFcmToken(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @RequestBody Map<String, String> request) {
        String fcmToken = request.get("fcmToken");
        if (fcmToken == null) {
            return ResponseEntity.badRequest().build();
        }
        User updatedUser = userService.updateFcmToken(firebaseUser.getUid(), fcmToken);
        return ResponseEntity.ok(userService.convertToDto(updatedUser));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserDto> getAllUsers() {
        return userService.findAllUsers().stream()
                .map(userService::convertToAdminDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserDto> getUserById(@PathVariable Long id) {
        return userService.findUserById(id)
                .map(userService::convertToAdminDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> updateUser(@PathVariable Long id, @RequestBody UserProfileDto userProfileDto) {
        try {
            UserProfileDto updatedUser = userService.updateUserProfile(id, userProfileDto);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String role = payload.get("role");
            if (role == null || role.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Role must be provided.");
            }
            UserProfileDto updatedUser = userService.updateUserRole(id, role);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Status must be provided.");
            }
            UserProfileDto updatedUser = userService.updateUserStatus(id, status);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}