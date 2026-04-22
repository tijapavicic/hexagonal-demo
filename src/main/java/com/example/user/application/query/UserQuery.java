package com.example.user.application.query;

/**
 * Encapsulates all filter and pagination parameters for a user list query.
 * Using a value object here avoids primitive obsession across the port and
 * service boundaries, and makes it trivial to add new filter criteria later.
 */
public record UserQuery(
        String name,
        Integer minAge,
        Integer maxAge,
        int page,
        int size
) {
    public UserQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
    }

    public static UserQuery of(String name, Integer minAge, Integer maxAge, int page, int size) {
        return new UserQuery(name, minAge, maxAge, page, size);
    }
}

