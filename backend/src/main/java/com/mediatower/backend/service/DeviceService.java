package com.mediatower.backend.service;
import com.mediatower.backend.model.KnownDevice;
import com.mediatower.backend.model.User;
import com.mediatower.backend.repository.KnownDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
@Service
public class DeviceService {


    @Autowired
    private KnownDeviceRepository knownDeviceRepository;

    @Autowired
    private EmailService emailService;

    @Async // Pour ne pas bloquer la connexion
    public void handleDeviceVerification(User user, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "Unknown Device";
        }

        String ipAddress = getClientIpAddress(request);

        Optional<KnownDevice> deviceOpt = knownDeviceRepository.findByUserAndUserAgent(user, userAgent);

        if (deviceOpt.isEmpty()) {
            // Appareil inconnu
            KnownDevice newDevice = new KnownDevice(user, userAgent, ipAddress);
            knownDeviceRepository.save(newDevice);

            // Envoyer l'e-mail d'alerte
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
            String loginTime = LocalDateTime.now().format(formatter);

            emailService.sendNewDeviceLoginAlertEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    loginTime,
                    ipAddress,
                    userAgent
            );
        } else {
            // Appareil connu, mettre à jour la dernière connexion
            KnownDevice knownDevice = deviceOpt.get();
            knownDevice.setLastIpAddress(ipAddress);
            knownDevice.setLastLogin(LocalDateTime.now());
            knownDeviceRepository.save(knownDevice);
        }
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