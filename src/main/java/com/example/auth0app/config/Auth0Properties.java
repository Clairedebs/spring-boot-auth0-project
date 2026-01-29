package com.example.auth0app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth0")
@Getter
@Setter
public class Auth0Properties {
    
    private String domain;
    private String clientId;
    private String clientSecret;
    private String audience;
    private String connection = "Username-Password-Authentication";
}
