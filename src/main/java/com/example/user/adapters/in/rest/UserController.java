package com.example.user.adapters.in.rest;

import com.example.user.adapters.in.rest.dto.CreateUserRequest;
import com.example.user.adapters.in.rest.dto.UpdateUserRequest;
import com.example.user.adapters.in.rest.dto.UserResponse;
import com.example.user.application.port.in.CreateUserUseCase;
import com.example.user.application.port.in.DeleteUserUseCase;
import com.example.user.application.port.in.GetUserUseCase;
import com.example.user.application.port.in.ListUsersUseCase;
import com.example.user.application.port.in.UpdateUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserRestMapper mapper;

    public UserController(CreateUserUseCase createUserUseCase,
                          GetUserUseCase getUserUseCase,
                          ListUsersUseCase listUsersUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UserRestMapper mapper) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return mapper.toResponse(createUserUseCase.create(mapper.toDomain(request)));
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable String id) {
        return mapper.toResponse(getUserUseCase.getById(id));
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return listUsersUseCase.getAll().stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return mapper.toResponse(updateUserUseCase.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        deleteUserUseCase.delete(id);
    }
}

