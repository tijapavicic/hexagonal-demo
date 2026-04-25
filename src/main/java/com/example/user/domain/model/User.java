package com.example.user.domain.model;

/**
 * User aggregate root. Self-validates on construction so that no invalid User
 * can exist regardless of which adapter (REST, CLI, Kafka…) created it.
 */
public record User(String id, String name, String address, Integer age) {

    public User {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name must not be blank");
        if (address == null || address.isBlank())
            throw new IllegalArgumentException("address must not be blank");
        if (age != null && (age < 0 || age > 150))
            throw new IllegalArgumentException("age must be between 0 and 150");
    }
}
