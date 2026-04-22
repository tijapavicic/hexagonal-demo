package com.example.user.adapters.out.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB persistence document for the users collection.
 * Using a record removes ~40 lines of boilerplate while Spring Data MongoDB
 * (4.x / Spring Boot 3.x) fully supports records for both reads and writes.
 */
@Document(collection = "users")
public record UserDocument(
        @Id String id,
        @Indexed String name,
        String address,
        Integer age
) {
}
