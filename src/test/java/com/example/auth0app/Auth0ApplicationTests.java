package com.example.auth0app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "auth0.domain=test-domain.auth0.com",
    "auth0.audience=test-audience",
    "auth0.management.client-id=test-client-id",
    "auth0.management.client-secret=test-client-secret"
})
class Auth0ApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
    }
}
