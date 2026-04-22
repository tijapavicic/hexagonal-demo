package com.example.user.domain.model;

import java.util.List;

public record UserPage(List<User> content, long totalElements, int totalPages, int page, int size) {
}

