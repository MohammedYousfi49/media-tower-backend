// Fichier : src/main/java/com/mediatower/backend/service/NotificationService.java (COMPLET ET MIS À JOUR)

package com.mediatower.backend.service;

import com.mediatower.backend.dto.NotificationDto;
import com.mediatower.backend.model.Notification;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.NotificationRepository;
import com.mediatower.backend.repository.UserRepository; // <-- AJOUT
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository; // <-- AJOUT

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) { // <-- MODIFIÉ
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository; // <-- MODIFIÉ
    }

    // --- ANCIENNE MÉTHODE RENOMMÉE ---
    @Transactional
    public void createNotificationForUser(User user, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    // --- NOUVELLE MÉTHODE ---
    @Transactional
    public void createUserNotification(String userUid, String message, String type) {
        // Trouve l'utilisateur par son UID et lui crée une notification
        userRepository.findByUid(userUid).ifPresent(user -> {
            createNotificationForUser(user, message, type);
        });
    }

    @Transactional
    public void createAdminNotification(String message, String type) {
        Notification notification = new Notification();
        notification.setUser(null); // Les notifications admin n'ont pas d'utilisateur spécifique
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    public List<NotificationDto> getAllNotifications() {
        return notificationRepository.findAllByOrderByIdDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
        return convertToDto(notification);
    }

    public List<NotificationDto> getNotificationsByUser(User user) {
        return notificationRepository.findByUserOrderByIdDesc(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUser() != null ? notification.getUser().getUid() : null);
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}