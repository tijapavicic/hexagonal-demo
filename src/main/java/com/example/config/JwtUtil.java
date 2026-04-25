package com.example.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Stateless JWT utility — token creation and validation.
 * The signing key is loaded from the {@code app.jwt.secret} property
 * (must be at least 256 bits / 32 chars). Never hardcoded.
 */
@Component
public class JwtUtil {

    private static final long EXPIRY_MS = 24 * 60 * 60 * 1_000L; // 24 h

    private final SecretKey signingKey;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Generate a signed JWT for {@code username}. */
    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRY_MS))
                .signWith(signingKey)
                .compact();
    }

    /** Extract the subject (username) from a valid token. Throws on invalid / expired token. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns {@code true} if the token is structurally valid and not expired. */
    public boolean isValid(String token) {
        try {
            Date expiry = parseClaims(token).getExpiration();
            return expiry.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

