package com.example.user.adapters.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "address is required") String address,
        @NotNull(message = "age is required")
        @Min(value = 0, message = "age must be at least 0")
        @Max(value = 150, message = "age must be at most 150")
        Integer age
) {
}

