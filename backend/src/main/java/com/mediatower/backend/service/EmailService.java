package com.mediatower.backend.service;

import com.mediatower.backend.model.Booking;
import com.mediatower.backend.model.Order;
import com.mediatower.backend.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail = "no-reply@mediatower.com";

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    private String buildUrl(String path) {
        return frontendBaseUrl + path;
    }

    // --- E-MAILS HTML ---
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        logger.info("Preparing to send order confirmation email for order #{}", order.getId());
        try {
            Context context = new Context();
            context.setVariable("userName", order.getUser().getFirstName());
            context.setVariable("orderId", order.getId());
            context.setVariable("orderDate", order.getOrderDate());
            context.setVariable("orderItems", order.getOrderItems().stream().map(this::toDto).collect(Collectors.toList()));
            context.setVariable("totalAmount", order.getTotalAmount());
            context.setVariable("accountUrl", buildUrl("/account"));

            String htmlContent = templateEngine.process("order-confirmation", context);
            if (htmlContent == null || htmlContent.isBlank()) {
                logger.error("Generated HTML content for order confirmation is empty for order #{}.", order.getId());
                return;
            }

            sendHtmlEmail(order.getUser().getEmail(), "Your MediaTower Order Confirmation (#" + order.getId() + ")", htmlContent);
            logger.info("Successfully sent order confirmation email to {}", order.getUser().getEmail());
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email for order #{}", order.getId(), e);
        }
    }
    @Async
    public void sendServiceInProgressEmail(Booking booking) {
        logger.info("Preparing to send service in-progress email for booking #{}", booking.getId());
        try {
            Context context = new Context();
            context.setVariable("customerName", booking.getCustomer().getFirstName());
            context.setVariable("serviceName", booking.getService().getNames().getOrDefault("en", "Your Service"));
            context.setVariable("bookingId", "#" + booking.getId());
            context.setVariable("accountUrl", buildUrl("/account"));

            String htmlContent = templateEngine.process("service-confirmation", context);
            if (htmlContent == null || htmlContent.isBlank()) {
                logger.error("Generated HTML for service confirmation is empty for booking #{}.", booking.getId());
                return;
            }

            sendHtmlEmail(booking.getCustomer().getEmail(), "Work has begun on your service booking (#" + booking.getId() + ")", htmlContent);
            logger.info("Successfully sent service in-progress email for booking #{}", booking.getId());
        } catch (Exception e) {
            logger.error("Failed to send service in-progress email for booking #{}", booking.getId(), e);
        }
    }
    @Async
    public void sendServiceCancelledEmail(Booking booking) {
        logger.info("Preparing to send service cancellation email for booking #{}", booking.getId());
        try {
            Context context = new Context();
            context.setVariable("customerName", booking.getCustomer().getFirstName());
            context.setVariable("serviceName", booking.getService().getNames().getOrDefault("en", "Your Service"));
            context.setVariable("bookingId", booking.getId());
            context.setVariable("accountUrl", buildUrl("/account"));

            String htmlContent = templateEngine.process("service-cancelled", context);
            if (htmlContent == null || htmlContent.isBlank()) {
                logger.error("Generated HTML for service cancellation is empty for booking #{}.", booking.getId());
                return;
            }

            sendHtmlEmail(booking.getCustomer().getEmail(), "Important Update Regarding Your Booking (#" + booking.getId() + ")", htmlContent);
            logger.info("Successfully sent service cancellation email for booking #{}", booking.getId());
        } catch (Exception e) {
            logger.error("Failed to send service cancellation email for booking #{}", booking.getId(), e);
        }
    }


    // --- E-MAILS TEXTE (AMÉLIORÉS) ---
    @Async
    public void sendBookingRequestedEmail(String customerName, String customerEmail, String serviceName) {
        String subject = "We've received your request for: " + serviceName;
        String text = String.format(
                "Hello %s,\n\n" +
                        "This is a confirmation that we have received your service request for '%s'.\n\n" +
                        "Our team will review it shortly and get back to you with the next steps.\n\n" +
                        "Best regards,\nThe Media Tower Team",
                customerName, serviceName
        );
        sendSimpleTextEmail(customerEmail, subject, text);
    }
    @Async
    public void sendBookingConfirmedEmail(String customerName, String customerEmail, String serviceName, Long bookingId) {
        String subject = "Your Booking for '" + serviceName + "' is Confirmed!";
        String text = String.format(
                "Hello %s,\n\n" +
                        "Good news! Your request for the service '%s' (Ref: #%d) has been approved.\n\n" +
                        "To finalize your booking, please proceed with the payment using the secure link below:\n\n" +
                        "%s\n\n" +
                        "This link is valid for 48 hours.\n\n" +
                        "Best regards,\nThe Media Tower Team",
                customerName, serviceName, bookingId, buildUrl("/checkout/booking/" + bookingId)
        );
        sendSimpleTextEmail(customerEmail, subject, text);
    }
    @Async
    public void sendBookingCompletedEmail(String customerName, String customerEmail, String serviceName, Long bookingId, Long serviceId) {
        String subject = "Your service '" + serviceName + "' is complete!";
        String text = String.format(
                "Hello %s,\n\n" +
                        "We are pleased to inform you that your service '%s' (Ref: #%d) has been completed.\n\n" +
                        "We hope you enjoyed the experience. Your feedback is very important to us! Please take a moment to leave a review on the service page:\n\n" +
                        "%s\n\n" +
                        "You can view all your service details in your account:\n" +
                        "%s\n\n" +
                        "Thank you for your trust,\nThe Media Tower Team",
                customerName, serviceName, bookingId, buildUrl("/services/" + serviceId + "#reviews"), buildUrl("/account")
        );
        sendSimpleTextEmail(customerEmail, subject, text);
    }
    @Async
    public void sendBookingCancelledBySystemEmail(String customerName, String customerEmail, String serviceName, Long bookingId) {
        String subject = "Update on your booking #" + bookingId;
        String text = String.format(
                "Hello %s,\n\n" +
                        "This is a notification regarding your provisional booking for the service: '%s'.\n\n" +
                        "Unfortunately, the 48-hour payment window has expired, and your booking has been automatically cancelled to free up the slot for other customers.\n\n" +
                        "If you are still interested, please feel free to make a new request on our website.\n\n" +
                        "Best regards,\nThe Media Tower Team",
                customerName, serviceName
        );
        sendSimpleTextEmail(customerEmail, subject, text);
    }

    // --- MÉTHODES UTILITAIRES POUR L'ENVOI ---
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setTo(to);
        helper.setFrom(fromEmail);
        mailSender.send(mimeMessage);
    }

    @Async
    public void sendSimpleTextEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // --- DTO INTERNE (INCHANGÉ) ---
    @Getter
    private static class OrderItemEmailDto {
        private final String productName;
        private final BigDecimal unitPrice;

        OrderItemEmailDto(String productName, BigDecimal unitPrice) {
            this.productName = productName;
            this.unitPrice = unitPrice;
        }
    }

    private OrderItemEmailDto toDto(OrderItem item) {
        String name = item.getProduct().getNames().getOrDefault("en", "Product Name Not Available");
        return new OrderItemEmailDto(name, item.getUnitPrice());
    }

    // --- E-MAILS DE SUPPORT (INCHANGÉS POUR L'INSTANT) ---
    @Async
    public void sendTicketReplyEmail(String userEmail, Long ticketId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(userEmail);
        message.setSubject("You have a new reply on your support ticket #" + ticketId);
        message.setText("Hello,\n\nA member of our team has replied to your support ticket.\n\nPlease log in to your account to view the message:\n\n" +
                "http://localhost:5174/account/tickets/" + ticketId);
        mailSender.send(message);
    }
    @Async
    public void sendTicketCreationConfirmationEmail(String userEmail, Long ticketId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(userEmail);
        message.setSubject("We have received your support request #" + ticketId);
        message.setText("Hello,\n\nThis is an automated confirmation that we have received your support request. A member of our team will get back to you as soon as possible.\n\nYou can view the status of your ticket here:\n\n" +
                "http://localhost:5174/account/tickets/" + ticketId);
        mailSender.send(message);
    }

    @Async
    public void sendTicketClosedEmail(String userEmail, Long ticketId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(userEmail);
        message.setSubject("Your support ticket #" + ticketId + " has been closed");
        message.setText("Hello,\n\nYour support ticket has been marked as resolved and closed.\n\nIf you feel your issue is not resolved, you can reply to this message to reopen the ticket.\n\nThank you for using Media Tower!\n\nThe Media Tower Team");
        mailSender.send(message);
    }
    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        logger.info("Preparing to send verification email to {}", to);
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("verificationUrl", buildUrl("/verify-email/" + token));

            String htmlContent = templateEngine.process("email-verification", context);
            sendHtmlEmail(to, "Welcome to MediaTower! Please verify your email", htmlContent);
            logger.info("Successfully sent verification email to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", to, e);
            // On ne relance pas l'exception ici pour ne pas polluer les logs d'AsyncUncaughtExceptionHandler
            // L'erreur est déjà enregistrée.
        }

    }
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String resetUrl) {
        logger.info("Preparing to send password reset email to {}", to);
        try {
            Context context = new Context();
            context.setVariable("userName", firstName);
            context.setVariable("resetUrl", resetUrl); // L'URL de réinitialisation

            // Nous allons créer un nouveau template HTML pour cet email
            String htmlContent = templateEngine.process("password-reset", context);
            sendHtmlEmail(to, "Reset Your MediaTower Password", htmlContent);
            logger.info("Successfully sent password reset email to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", to, e.getMessage(), e);
        }
    }
    /**
     * NOUVELLE MÉTHODE : Envoie une alerte pour une nouvelle connexion.
     * @param to L'email de l'utilisateur
     * @param userName Le nom de l'utilisateur
     * @param loginTime La date et l'heure de la connexion
     * @param ipAddress L'adresse IP de la connexion
     * @param userAgent Le navigateur/appareil utilisé
     */
    @Async // <-- Cette nouvelle méthode est aussi asynchrone
    public void sendNewDeviceLoginAlertEmail(String to, String userName, String loginTime, String ipAddress, String userAgent) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("loginTime", loginTime);
            context.setVariable("ipAddress", ipAddress);
            context.setVariable("userAgent", userAgent);

            String htmlContent = templateEngine.process("NewDeviceLoginAlertTemplate", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Alerte de Sécurité : Nouvelle connexion à votre compte MediaTower");
            helper.setText(htmlContent, true); // true pour indiquer que c'est du HTML

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            // Gérer l'erreur, par exemple avec un logger
            // logger.error("Failed to send new device login alert email to {}", to, e);
        }
    }
    @Async
    public void sendNewLocationLoginAlertEmail(String to, String userName, String loginTime, String ipAddress, String userAgent) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("loginTime", loginTime);
            context.setVariable("ipAddress", ipAddress);
            context.setVariable("userAgent", userAgent);

            String htmlContent = templateEngine.process("NewLocationLoginAlertTemplate", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Alerte de Sécurité : Connexion depuis une nouvelle localisation");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            logger.error("Failed to send new location login alert email to {}", to, e);
        }


    }}
