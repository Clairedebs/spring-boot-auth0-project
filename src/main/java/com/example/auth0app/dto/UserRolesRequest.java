package com.example.auth0app.dto;

import com.example.auth0app.validation.ValidRoles;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRolesRequest {

    @NotEmpty(message = "At least one role is required")
    @ValidRoles
    private Set<String> roles;
}
