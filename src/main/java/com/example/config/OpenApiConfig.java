package com.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Hexagonal Demo API",
                version = "1.0",
                description = "User CRUD service built with hexagonal architecture and Spring Boot"
        )
)
@Configuration
public class OpenApiConfig {
}

