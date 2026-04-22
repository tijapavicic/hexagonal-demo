package com.example.user.domain.exception;

/**
 * Domain exception raised when a requested {@code User} cannot be found by its identifier.
 * Kept in the domain layer because it expresses a domain rule violation,
 * not an infrastructure or application concern.
 */
public class UserNotFoundException extends RuntimeException {

    private final String userId;

    public UserNotFoundException(String userId) {
        super("User not found with id: " + userId);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}

