package com.example.user.application.port.in;

import com.example.user.domain.model.UserPage;

public interface ListUsersUseCase {
    UserPage getAll(String name, Integer minAge, Integer maxAge, int page, int size);
}

