package com.mediatower.backend.security;

import com.mediatower.backend.model.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.Collections;

@Getter
public class FirebaseUser extends User {

    private final String email;
    private final String uid;
    private final UserRole role;
    private final String firstName;
    private final String lastName;

    public FirebaseUser(String email, String uid, UserRole role, String firstName, String lastName, Collection<? extends GrantedAuthority> authorities) {
        super(email, "", authorities);
        this.email = email;
        this.uid = uid;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getName() {
        if (firstName != null && !firstName.isBlank()) {
            return firstName;
        }
        return email;
    }

    public static FirebaseUser create(com.mediatower.backend.model.User userInDb) {
        String roleName = "ROLE_" + userInDb.getRole().name();
        return new FirebaseUser(
                userInDb.getEmail(),
                userInDb.getUid(),
                userInDb.getRole(),
                userInDb.getFirstName(),
                userInDb.getLastName(),
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}