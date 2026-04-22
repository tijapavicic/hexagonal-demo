package com.example.user.application.port.out;

import com.example.user.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);

    Optional<User> findById(String id);

    List<User> findAll();

    void deleteById(String id);
}

