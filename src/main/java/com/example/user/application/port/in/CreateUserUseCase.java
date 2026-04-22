package com.example.user.application.port.in;

import com.example.user.domain.model.User;

public interface CreateUserUseCase {
    User create(User user);
}

