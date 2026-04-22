package com.example.user.application.port.out;

import com.example.user.domain.model.User;
import com.example.user.domain.model.UserPage;

import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);

    Optional<User> findById(String id);

    UserPage findAll(String name, Integer minAge, Integer maxAge, int page, int size);

    void deleteById(String id);
}

