package com.example.auth0app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidRolesValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoles {
    
    String message() default "Invalid role. Allowed roles are: USER, ADMIN";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
