package com.example.user.adapters.in.rest;

import com.example.user.adapters.in.rest.dto.CreateUserRequest;
import com.example.user.adapters.in.rest.dto.UpdateUserRequest;
import com.example.user.adapters.in.rest.dto.UserPageResponse;
import com.example.user.adapters.in.rest.dto.UserResponse;
import com.example.user.application.port.in.CreateUserUseCase;
import com.example.user.application.port.in.DeleteUserUseCase;
import com.example.user.application.port.in.GetUserUseCase;
import com.example.user.application.port.in.ListUsersUseCase;
import com.example.user.application.port.in.UpdateUserUseCase;
import com.example.user.domain.model.UserPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "User CRUD operations")
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

    @Operation(summary = "Create a new user")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return mapper.toResponse(createUserUseCase.create(mapper.toDomain(request)));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable String id) {
        return mapper.toResponse(getUserUseCase.getById(id));
    }

    @Operation(summary = "List users with optional filters and pagination")
    @GetMapping
    public UserPageResponse getAll(
            @Parameter(description = "Filter by name (case-insensitive, partial match)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Minimum age (inclusive)")
            @RequestParam(required = false) Integer minAge,
            @Parameter(description = "Maximum age (inclusive)")
            @RequestParam(required = false) Integer maxAge,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        UserPage result = listUsersUseCase.getAll(name, minAge, maxAge, page, size);
        return new UserPageResponse(
                result.content().stream().map(mapper::toResponse).toList(),
                result.totalElements(),
                result.totalPages(),
                result.page(),
                result.size());
    }

    @Operation(summary = "Update an existing user")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return mapper.toResponse(updateUserUseCase.update(id, mapper.toDomain(request)));
    }

    @Operation(summary = "Delete a user")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        deleteUserUseCase.delete(id);
    }
}

