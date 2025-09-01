package com.mediatower.backend.repository;

import com.mediatower.backend.model.KnownDevice;
import com.mediatower.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnownDeviceRepository extends JpaRepository<KnownDevice, Long> {
    Optional<KnownDevice> findByUserAndUserAgent(User user, String userAgent);
}