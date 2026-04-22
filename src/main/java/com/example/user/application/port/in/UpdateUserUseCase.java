package com.example.user.application.port.in;

import com.example.user.domain.model.User;

public interface UpdateUserUseCase {
    User update(String id, User user);
}

