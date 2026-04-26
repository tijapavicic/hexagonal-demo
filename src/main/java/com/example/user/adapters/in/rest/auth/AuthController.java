package com.example.user.adapters.in.rest.auth;

import com.example.config.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Auth inbound adapter — issues JWT tokens.
 * Credentials are validated against bcrypt-encoded values loaded from
 * {@code APP_USERNAME} / {@code APP_PASSWORD_HASH} environment variables
 * (set in application.yml via {@code app.auth.*} properties).
 * No credentials are hardcoded.
 */
@Tag(name = "Auth", description = "Obtain a JWT bearer token")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final String appUsername;
    private final String appPasswordHash;

    public AuthController(JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder,
                          @org.springframework.beans.factory.annotation.Value("${app.auth.username}") String appUsername,
                          @org.springframework.beans.factory.annotation.Value("${app.auth.password-hash}") String appPasswordHash) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.appUsername = appUsername;
        this.appPasswordHash = appPasswordHash;
    }

    @Operation(summary = "Login and receive a JWT bearer token")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (!appUsername.equals(request.username())
                || !passwordEncoder.matches(request.password(), appPasswordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return LoginResponse.bearer(jwtUtil.generateToken(request.username()));
    }
}
