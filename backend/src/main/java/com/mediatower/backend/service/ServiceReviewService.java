package com.mediatower.backend.service;

import com.mediatower.backend.dto.ServiceReviewDto;
import com.mediatower.backend.model.BookingStatus;
import com.mediatower.backend.model.Service; // Assurez-vous que l'import est correct
import com.mediatower.backend.model.ServiceReview;
import com.mediatower.backend.model.User;
import java.util.Optional;
import com.mediatower.backend.repository.BookingRepository;
import com.mediatower.backend.repository.ServiceRepository;
import com.mediatower.backend.repository.ServiceReviewRepository;
import com.mediatower.backend.repository.UserRepository;
import org.springframework.stereotype.Component; // Utilisez @Component ou @Service
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Component // @Service est aussi un @Component, les deux fonctionnent
public class ServiceReviewService {

    private final ServiceReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;

    // Le constructeur est correct, l'erreur venait probablement d'une version précédente
    public ServiceReviewService(ServiceReviewRepository reviewRepository, UserRepository userRepository, ServiceRepository serviceRepository, BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.bookingRepository = bookingRepository;
    }
    public Optional<ServiceReviewDto> getReviewById(Long id) {
        return reviewRepository.findById(id).map(this::convertToDto);
    }

    public List<ServiceReviewDto> getReviewsForService(Long serviceId) {
        return reviewRepository.findByServiceId(serviceId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceReviewDto createServiceReview(String userUid, Long serviceId, ServiceReviewDto dto) {
        User user = userRepository.findByUid(userUid)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + userUid));

        // La variable 'service' est de type com.mediatower.backend.model.Service
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with ID: " + serviceId));

        boolean hasCompletedBooking = bookingRepository.findAll().stream()
                .anyMatch(b -> b.getCustomer().getId().equals(user.getId()) &&
                        b.getService().getId().equals(serviceId) &&
                        b.getStatus() == BookingStatus.COMPLETED);

        if (!hasCompletedBooking) {
            throw new IllegalStateException("User cannot review a service they have not completed.");
        }

        ServiceReview review = new ServiceReview();
        review.setUser(user);
        // Ici, on assigne bien un objet 'Service' à un champ 'Service'
        review.setService(service);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        ServiceReview savedReview = reviewRepository.save(review);
        return convertToDto(savedReview);
    }

    private ServiceReviewDto convertToDto(ServiceReview review) {
        ServiceReviewDto dto = new ServiceReviewDto();
        dto.setId(review.getId());
        dto.setServiceId(review.getService().getId());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewDate(review.getReviewDate());
        return dto;
    }
    @Transactional
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Service review not found with ID: " + id);
        }
        reviewRepository.deleteById(id);
    }
}