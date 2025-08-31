package com.mediatower.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    /**
     * Tente d'envoyer une notification push à un appareil via son token FCM.
     * Cette méthode est essentielle pour la logique "Délivré" (✓✓) du chat.
     *
     * @param token Le token FCM de l'appareil cible.
     * @param title Le titre de la notification.
     * @param body Le corps du message de la notification.
     * @return {@code true} si l'envoi à Firebase a réussi, {@code false} sinon.
     */
    public boolean sendPushNotificationToToken(String token, String title, String body) {
        // Vérification de sécurité : ne pas tenter d'envoyer si le token est invalide.
        if (token == null || token.trim().isEmpty()) {
            logger.warn("FCM token is null or empty. Cannot send push notification.");
            return false;
        }

        // Construction de la notification.
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        // Construction du message complet à envoyer.
        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                // Vous pouvez ajouter des données personnalisées ici si le frontend en a besoin.
                // Par exemple : .putData("chatId", "some-chat-id")
                .build();

        try {
            // Tentative d'envoi du message via le service Firebase Messaging.
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent push notification. Response: {}", response);
            // Si aucune exception n'est levée, cela signifie que Firebase a accepté la requête.
            // On retourne true pour indiquer le succès de la livraison au serveur.
            return true;
        } catch (FirebaseMessagingException e) {
            // Une erreur est survenue (par exemple, le token est invalide, le service est indisponible).
            logger.error("Failed to send push notification to token [{}]: {}", token, e.getMessage());
            // On retourne false pour indiquer l'échec.
            return false;
        }
    }
}