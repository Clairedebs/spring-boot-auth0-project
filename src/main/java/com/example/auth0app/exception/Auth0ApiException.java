package com.example.auth0app.exception;

public class Auth0ApiException extends RuntimeException {
    
    public Auth0ApiException(String message) {
        super(message);
    }
    
    public Auth0ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
