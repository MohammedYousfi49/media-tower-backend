package com.mediatower.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "known_devices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "userAgent"})
})
@Getter
@Setter
@NoArgsConstructor
public class KnownDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String userAgent;

    @Column
    private String lastIpAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime firstLogin;

    @Column
    private LocalDateTime lastLogin;

    public KnownDevice(User user, String userAgent, String ipAddress) {
        this.user = user;
        this.userAgent = userAgent;
        this.lastIpAddress = ipAddress;
        this.lastLogin = LocalDateTime.now();
    }
}