package com.example.demo4.service;

import com.example.demo4.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthTokenServiceTest {

    private final AuthTokenService authTokenService =
            new AuthTokenService("hotel-management-test-secret", 1);

    @Test
    void createsAndVerifiesSignedToken() {
        User user = new User();
        user.setId(7L);
        user.setRole(1);

        String token = authTokenService.createToken(user);

        assertEquals(7L, authTokenService.verifyAndGetUserId(token));
    }

    @Test
    void rejectsTamperedToken() {
        User user = new User();
        user.setId(7L);
        user.setRole(1);
        String token = authTokenService.createToken(user);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThrows(IllegalArgumentException.class,
                () -> authTokenService.verifyAndGetUserId(tampered));
    }
}
