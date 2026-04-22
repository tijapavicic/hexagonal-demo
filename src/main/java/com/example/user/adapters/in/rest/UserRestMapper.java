package com.example.user.adapters.in.rest;

import com.example.user.adapters.in.rest.dto.CreateUserRequest;
import com.example.user.adapters.in.rest.dto.UpdateUserRequest;
import com.example.user.adapters.in.rest.dto.UserResponse;
import com.example.user.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserRestMapper {

    public User toDomain(CreateUserRequest request) {
        return new User(null, request.name(), request.address(), request.age());
    }

    public User toDomain(UpdateUserRequest request) {
        return new User(null, request.name(), request.address(), request.age());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.name(), user.address(), user.age());
    }
}

