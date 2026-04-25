package com.example.user.application.port.out;

import com.example.user.application.query.UserQuery;
import com.example.user.domain.model.User;
import com.example.user.domain.model.UserPage;

import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);

    Optional<User> findById(String id);

    boolean existsById(String id);

    UserPage findAll(UserQuery query);

    void deleteById(String id);
}
