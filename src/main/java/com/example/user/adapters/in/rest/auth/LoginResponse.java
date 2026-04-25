package com.example.user.adapters.in.rest.auth;

public record LoginResponse(String token, String type, long expiresInSeconds) {
    public static LoginResponse bearer(String token) {
        return new LoginResponse(token, "Bearer", 86_400L);
    }
}

