package com.mediatower.backend.service;

import com.mediatower.backend.dto.BookingDto;
import com.mediatower.backend.model.*;
import com.mediatower.backend.repository.BookingRepository;
import com.mediatower.backend.repository.ServiceRepository;
import com.mediatower.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, ServiceRepository serviceRepository, EmailService emailService, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    public BookingDto convertToDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setCustomerName(booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName());
        dto.setCustomerEmail(booking.getCustomer().getEmail());
        dto.setServiceName(booking.getService().getNames().getOrDefault("en", "N/A"));
        dto.setStatus(booking.getStatus().name());
        if (booking.getAssignedAdmin() != null) {
            dto.setAssignedAdminName(booking.getAssignedAdmin().getFirstName() + " " + booking.getAssignedAdmin().getLastName());
        }
        dto.setCustomerNotes(booking.getCustomerNotes());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setServicePrice(booking.getService().getPrice());

        // ====================== CORRECTION DE L'ERREUR DE COMPILATION ======================
        // On utilise le bon nom de méthode : getMediaAssets()
        // Et les bons accesseurs : media.getType() et media.isPrimary()
        String baseUrl = "http://localhost:8080/api/download/";
        String imageUrl = booking.getService().getMediaAssets().stream()
                .filter(media -> media.getType() == MediaType.IMAGE && media.isPrimary())
                .findFirst()
                .map(media -> baseUrl + media.getFileName())
                .orElseGet(() -> booking.getService().getMediaAssets().stream()
                        .filter(media -> media.getType() == MediaType.IMAGE)
                        .findFirst()
                        .map(media -> baseUrl + media.getFileName())
                        .orElse(null)
                );
        dto.setServiceImageUrl(imageUrl);
        // =================================================================================

        return dto;
    }

    @Transactional
    public void confirmBookingPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            booking.setPaymentDueDate(null);
            bookingRepository.save(booking);
            emailService.sendServiceInProgressEmail(booking);
            notificationService.createAdminNotification("Payment received for booking #" + booking.getId() + ". Work can begin.", "BOOKING_PAID");
        }
    }

    @Transactional
    public BookingDto updateBookingStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
        booking.setStatus(newStatus);

        if (newStatus == BookingStatus.CONFIRMED) {
            booking.setPaymentDueDate(LocalDateTime.now().plusHours(48));
        }

        Booking updatedBooking = bookingRepository.save(booking);
        String serviceName = updatedBooking.getService().getNames().getOrDefault("en", "your service");
        User customer = updatedBooking.getCustomer();

        if (newStatus == BookingStatus.CONFIRMED) {
            emailService.sendBookingConfirmedEmail(customer.getFirstName(), customer.getEmail(), serviceName, bookingId);
            notificationService.createAdminNotification("Booking #" + bookingId + " confirmed. Awaiting payment.", "BOOKING_UPDATE");
        } else if (newStatus == BookingStatus.COMPLETED) {
            emailService.sendBookingCompletedEmail(customer.getFirstName(), customer.getEmail(), serviceName, bookingId, updatedBooking.getService().getId());
            notificationService.createAdminNotification("Booking #" + bookingId + " has been marked as completed.", "BOOKING_UPDATE");
        } else if (newStatus == BookingStatus.CANCELLED) {
            emailService.sendServiceCancelledEmail(updatedBooking);
            notificationService.createAdminNotification("Booking #" + bookingId + " has been cancelled.", "BOOKING_UPDATE");
        }
        return convertToDto(updatedBooking);
    }

    @Transactional
    public BookingDto unassignAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
        booking.setAssignedAdmin(null);
        booking.setStatus(BookingStatus.PENDING);
        Booking savedBooking = bookingRepository.save(booking);
        notificationService.createAdminNotification("Booking #" + bookingId + " has been unassigned and is available again.", "BOOKING_UPDATE");
        return convertToDto(savedBooking);
    }

    @Transactional(readOnly = true)
    public Page<BookingDto> getAllBookingsPaginated(String search, String filter, String adminUid, Pageable pageable) {
        String searchTerm = (search == null || search.trim().isEmpty()) ? null : search;
        BookingStatus statusFilter = null;
        Long adminIdFilter = null;

        if ("NEW".equals(filter)) {
            statusFilter = BookingStatus.PENDING;
        } else if ("MY_QUEUE".equals(filter) && adminUid != null) {
            // C'est ici que nous convertissons l'UID en ID de base de données
            adminIdFilter = userRepository.findByUid(adminUid)
                    .map(User::getId)
                    .orElse(-1L); // Utilise -1L pour ne jamais rien trouver si l'admin n'existe pas
        }

        Page<Booking> bookingPage = bookingRepository.findWithFilters(searchTerm, statusFilter, adminIdFilter, pageable);
        return bookingPage.map(this::convertToDto);
    }

    @Transactional
    public BookingDto createBooking(String userUid, Long serviceId, String notes) {
        User customer = userRepository.findByUid(userUid).orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));
        com.mediatower.backend.model.Service service = serviceRepository.findById(serviceId).orElseThrow(() -> new RuntimeException("Service not found with ID: " + serviceId));

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setCustomerNotes(notes);
        booking.setStatus(BookingStatus.PENDING);
        Booking savedBooking = bookingRepository.save(booking);

        String serviceName = service.getNames().getOrDefault("en", "the requested service");
        emailService.sendBookingRequestedEmail(customer.getFirstName(), customer.getEmail(), serviceName);
        notificationService.createAdminNotification("New booking request from " + customer.getEmail() + " for '" + serviceName + "'", "NEW_BOOKING");

        return convertToDto(savedBooking);
    }

    @Transactional
    public BookingDto assignBookingToAdmin(Long bookingId, String adminUid) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
        User admin = userRepository.findByUid(adminUid).orElseThrow(() -> new RuntimeException("Admin User not found with UID: " + adminUid));

        booking.setAssignedAdmin(admin);
        if(booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.PROCESSING);
        }

        return convertToDto(bookingRepository.save(booking));
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cancelExpiredBookings() {
        List<Booking> expiredBookings = bookingRepository.findByStatusAndPaymentDueDateBefore(BookingStatus.CONFIRMED, LocalDateTime.now());
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            String serviceName = booking.getService().getNames().getOrDefault("en", "your service");
            User customer = booking.getCustomer();
            emailService.sendBookingCancelledBySystemEmail(customer.getFirstName(), customer.getEmail(), serviceName, booking.getId());
            notificationService.createAdminNotification("Booking #" + booking.getId() + " was auto-cancelled (payment expired).", "BOOKING_CANCELLED");
        }
    }

    public List<BookingDto> getBookingsByUserId(String userUid) {
        User customer = userRepository.findByUid(userUid).orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));
        return bookingRepository.findByCustomer(customer).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public BookingDto getBookingDtoById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
        return convertToDto(booking);
    }
}