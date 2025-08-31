// Chemin : src/main/java/com/mediatower/backend/repository/NotificationRepository.java

package com.mediatower.backend.repository;

import com.mediatower.backend.model.Notification;
import com.mediatower.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // =========== CORRECTION : Remplacer "IsRead" par "Read" ===========
    // Le nom de la méthode doit correspondre exactement au nom du champ dans l'entité Notification.
    List<Notification> findByUserAndRead(User user, boolean read);

    List<Notification> findByUser(User user);

    List<Notification> findAllByOrderByIdDesc();

    List<Notification> findByUserOrderByIdDesc(User user);
}