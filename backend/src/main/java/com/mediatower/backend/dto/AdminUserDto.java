// Fichier : src/main/java/com/mediatower/backend/dto/AdminUserDto.java (NOUVEAU)

package com.mediatower.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String uid;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String status;
    private String phoneNumber;
    private String address;
}