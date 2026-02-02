package com.example.auth0app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValidRolesValidator implements ConstraintValidator<ValidRoles, Set<String>> {

    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList("USER", "ADMIN"));

    @Override
    public boolean isValid(Set<String> roles, ConstraintValidatorContext context) {
        if (roles == null || roles.isEmpty()) {
            return true; // Let @NotEmpty handle null/empty validation
        }
        
        return roles.stream().allMatch(VALID_ROLES::contains);
    }
}
