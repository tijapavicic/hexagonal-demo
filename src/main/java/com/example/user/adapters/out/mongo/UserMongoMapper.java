package com.example.user.adapters.out.mongo;

import com.example.user.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMongoMapper {

    public UserDocument toDocument(User user) {
        return new UserDocument(user.id(), user.name(), user.address(), user.age());
    }

    public User toDomain(UserDocument document) {
        return new User(document.getId(), document.getName(), document.getAddress(), document.getAge());
    }
}

