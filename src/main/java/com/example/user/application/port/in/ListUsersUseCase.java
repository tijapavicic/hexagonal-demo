package com.example.user.application.port.in;

import com.example.user.application.query.UserQuery;
import com.example.user.domain.model.UserPage;

public interface ListUsersUseCase {
    UserPage getAll(UserQuery query);
}
