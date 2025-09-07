package com.mediatower.backend.service;

import com.mediatower.backend.model.KnownDevice;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.KnownDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Service
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    private final KnownDeviceRepository knownDeviceRepository;
    private final EmailService emailService;

    // Injection par constructeur (meilleure pratique)
    public DeviceService(KnownDeviceRepository knownDeviceRepository, EmailService emailService) {
        this.knownDeviceRepository = knownDeviceRepository;
        this.emailService = emailService;
    }

    @Async
    @Transactional // Ajout de @Transactional pour garantir la cohérence des données
    public void handleDeviceVerification(User user, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "Unknown Device";
        }
        // Pour éviter les user-agents trop longs
        if (userAgent.length() > 512) {
            userAgent = userAgent.substring(0, 511);
        }

        String ipAddress = getClientIpAddress(request);

        Optional<KnownDevice> deviceOpt = knownDeviceRepository.findByUserAndUserAgent(user, userAgent);

        if (deviceOpt.isEmpty()) {
            // --- CAS A : Appareil totalement inconnu ---
            logger.info("New device detected for user {}. User-Agent: {}", user.getEmail(), userAgent);
            KnownDevice newDevice = new KnownDevice(user, userAgent, ipAddress);
            knownDeviceRepository.save(newDevice);
            sendNewDeviceAlert(user, newDevice);

        } else {
            // --- CAS B : Appareil déjà connu ---
            KnownDevice knownDevice = deviceOpt.get();

            // On vérifie si l'adresse IP a changé de manière significative
            if (!Objects.equals(knownDevice.getLastIpAddress(), ipAddress)) {
                logger.info("New IP address detected for a known device for user {}. Old IP: {}, New IP: {}",
                        user.getEmail(), knownDevice.getLastIpAddress(), ipAddress);

                // On met à jour l'IP et l'heure de connexion
                knownDevice.setLastIpAddress(ipAddress);
                knownDevice.setLastLogin(LocalDateTime.now());
                knownDeviceRepository.save(knownDevice);

                // On envoie une alerte de nouvelle localisation
                sendNewLocationAlert(user, knownDevice);
            } else {
                // L'IP est la même, on met juste à jour l'heure de connexion
                knownDevice.setLastLogin(LocalDateTime.now());
                knownDeviceRepository.save(knownDevice);
            }
        }
    }

    // --- NOUVELLES MÉTHODES PRIVÉES POUR ENVOYER LES E-MAILS ---

    private void sendNewDeviceAlert(User user, KnownDevice device) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        String loginTime = device.getLastLogin().format(formatter);

        // Nous allons créer une nouvelle méthode dans EmailService pour ce template
        emailService.sendNewDeviceLoginAlertEmail(
                user.getEmail(),
                user.getFirstName(),
                loginTime,
                device.getLastIpAddress(),
                device.getUserAgent()
        );
    }

    private void sendNewLocationAlert(User user, KnownDevice device) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
        String loginTime = device.getLastLogin().format(formatter);

        // Nous allons créer une autre nouvelle méthode dans EmailService pour ce template
        emailService.sendNewLocationLoginAlertEmail(
                user.getEmail(),
                user.getFirstName(),
                loginTime,
                device.getLastIpAddress(),
                device.getUserAgent()
        );
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "N/A";
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}