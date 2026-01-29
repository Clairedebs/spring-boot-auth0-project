package com.example.auth0app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "auth0.domain=test-domain.auth0.com",
    "auth0.clientId=test-client-id",
    "auth0.clientSecret=test-client-secret",
    "auth0.audience=test-audience"
})
class Auth0ApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
    }
}
