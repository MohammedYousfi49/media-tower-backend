package com.mediatower.backend.controller;

import com.mediatower.backend.dto.*;
import com.mediatower.backend.model.User;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.OrderService;
import com.mediatower.backend.service.ReviewService;
import com.mediatower.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // === CORRECTION 1: Ajout de la déclaration du logger ===
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

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

//    @PutMapping("/me/password")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<?> changeCurrentUserPassword(
//            @AuthenticationPrincipal FirebaseUser firebaseUser,
//            @RequestBody Map<String, String> payload,
//            HttpServletRequest request) {
//
//        logger.info("Password change request received for user: {}", firebaseUser.getEmail());
//
//        String oldPassword = payload.get("oldPassword");
//        String newPassword = payload.get("newPassword");
//
//        if (oldPassword == null || oldPassword.trim().isEmpty()) {
//            logger.warn("Missing oldPassword for user: {}", firebaseUser.getEmail());
//            return ResponseEntity.badRequest().body(Map.of("error", "Old password is required."));
//        }
//
//        if (newPassword == null || newPassword.trim().isEmpty()) {
//            logger.warn("Missing newPassword for user: {}", firebaseUser.getEmail());
//            return ResponseEntity.badRequest().body(Map.of("error", "New password is required."));
//        }
//
//        try {
//            User user = userService.findUserByEmail(firebaseUser.getEmail())
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            logger.info("Attempting password change for user: {}", user.getEmail());
//            userService.changePasswordFromProfile(user, oldPassword, newPassword, request);
//
//            logger.info("Password changed successfully for user: {}", user.getEmail());
//            return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
//        } catch (IllegalArgumentException e) {
//            logger.warn("Password change failed for user {}: {}", firebaseUser.getEmail(), e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
//        } catch (Exception e) {
//            logger.error("Error changing password for user {}: {}", firebaseUser.getEmail(), e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal error occurred."));
//        }
//    }
    // === CORRECTION 2: Suppression de la méthode en double. On ne garde que celle-ci. ===
    @GetMapping("/me/password-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PasswordHistoryDto>> getPasswordHistory(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        try {
            User user = userService.findUserByEmail(firebaseUser.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<PasswordHistoryDto> history = userService.getPasswordHistoryForUser(user);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching password history for user {}: {}", firebaseUser.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
    public ResponseEntity<Page<AdminUserDto>> getAllUsers(
            @RequestParam(required = false) String search, // Paramètre pour la recherche
            Pageable pageable // Paramètres pour la pagination (page, size, sort)
    ) {
        Page<AdminUserDto> users = userService.findAllUsersPaginated(search, pageable);
        return ResponseEntity.ok(users);
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
    @PostMapping("/me/profile-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadProfileImage(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a file to upload."));
        }

        try {
            UserProfileDto updatedUser = userService.updateUserProfileImage(firebaseUser.getEmail(), file);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("Could not upload profile image for user {}: {}", firebaseUser.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to upload image."));
        }
    }
    @GetMapping("/me/login-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LoginHistoryDto>> getLoginHistory(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        User user = userService.findUserByEmail(firebaseUser.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<LoginHistoryDto> history = userService.getLoginHistoryForUser(user);
        return ResponseEntity.ok(history);
    }
}