package com.example.user.adapters.out.mongo;

import com.example.user.domain.model.User;

/**
 * Pure mapping utility — no state, no dependencies.
 * Declared final with a private constructor to prevent instantiation.
 * Static methods are more honest than injecting a stateless Spring bean.
 */
public final class UserMongoMapper {

    private UserMongoMapper() {}

    public static UserDocument toDocument(User user) {
        return new UserDocument(user.id(), user.name(), user.address(), user.age());
    }

    public static User toDomain(UserDocument document) {
        return new User(document.id(), document.name(), document.address(), document.age());
    }
}
