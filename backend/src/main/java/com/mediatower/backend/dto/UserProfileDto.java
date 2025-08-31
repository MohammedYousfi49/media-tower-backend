package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private String preferredLanguage;
    private String phoneNumber;
    private String address;
    private boolean emailVerified;
    private boolean mfaEnabled;
}