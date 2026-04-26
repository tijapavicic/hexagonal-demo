package com.example.config;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "jwt-test-secret-with-at-least-32-chars";

    @Test
    void generateTokenShouldProduceValidTokenForUsername() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);

        String token = jwtUtil.generateToken("alice");

        assertNotNull(token);
        assertEquals("alice", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void isValidShouldReturnFalseForTamperedToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        String token = jwtUtil.generateToken("alice");
        String tamperedToken = token + "tampered";

        assertFalse(jwtUtil.isValid(tamperedToken));
    }

    @Test
    void extractUsernameShouldThrowForTamperedToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET);
        String token = jwtUtil.generateToken("alice");
        String tamperedToken = token + "tampered";

        assertThrows(Exception.class, () -> jwtUtil.extractUsername(tamperedToken));
    }

    @Test
    void constructorShouldRejectWeakSecret() {
        assertThrows(WeakKeyException.class, () -> new JwtUtil("short-secret"));
    }
}
