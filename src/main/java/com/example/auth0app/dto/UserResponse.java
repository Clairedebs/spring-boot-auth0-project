package com.example.auth0app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String userId;  // Auth0 user ID
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean emailVerified;
}
