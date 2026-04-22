package com.example.user.application.port.in;

import com.example.user.domain.model.User;

import java.util.List;

public interface ListUsersUseCase {
    List<User> getAll();
}

