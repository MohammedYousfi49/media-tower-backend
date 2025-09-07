package com.mediatower.backend.controller;

import com.mediatower.backend.dto.BookingDto;
import com.mediatower.backend.model.BookingStatus;
import com.mediatower.backend.security.FirebaseUser;
import com.mediatower.backend.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }



    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingDto>> getMyBookings(@AuthenticationPrincipal FirebaseUser firebaseUser) {
        List<BookingDto> userBookings = bookingService.getBookingsByUserId(firebaseUser.getUid());
        return ResponseEntity.ok(userBookings);
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BookingDto>> getAllBookings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String filter,
            @AuthenticationPrincipal FirebaseUser adminUser,
            Pageable pageable) {

        // CORRECTION : On passe maintenant le getUid() au lieu de getId()
        Page<BookingDto> bookings = bookingService.getAllBookingsPaginated(search, filter, adminUser.getUid(), pageable);
        return ResponseEntity.ok(bookings);
    }

    // --- NOUVEL ENDPOINT POUR LA PAGE DE CHECKOUT ---
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDto> getBookingById(@PathVariable Long bookingId) {
        // Note: il faudra créer la méthode getBookingDtoById dans le service
        try {
            BookingDto booking = bookingService.getBookingDtoById(bookingId);
            return ResponseEntity.ok(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/service/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingDto> createBooking(
            @AuthenticationPrincipal FirebaseUser firebaseUser,
            @PathVariable Long serviceId,
            @RequestBody(required = false) Map<String, String> payload) {
        String notes = (payload != null) ? payload.get("notes") : "";
        BookingDto createdBooking = bookingService.createBooking(firebaseUser.getUid(), serviceId, notes);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @PutMapping("/{bookingId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingDto> assignBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal FirebaseUser adminUser) {
        try {
            BookingDto assignedBooking = bookingService.assignBookingToAdmin(bookingId, adminUser.getUid());
            return ResponseEntity.ok(assignedBooking);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{bookingId}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingDto> unassignAdmin(@PathVariable Long bookingId) {
        try {
            BookingDto unassignedBooking = bookingService.unassignAdmin(bookingId);
            return ResponseEntity.ok(unassignedBooking);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingDto> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> payload) {
        try {
            String statusStr = payload.get("status");
            if (statusStr == null || statusStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }
            BookingStatus newStatus = BookingStatus.valueOf(statusStr.toUpperCase());
            BookingDto updatedBooking = bookingService.updateBookingStatus(bookingId, newStatus);
            return ResponseEntity.ok(updatedBooking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}