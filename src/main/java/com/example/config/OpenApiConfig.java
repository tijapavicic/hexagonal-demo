package com.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Hexagonal Demo API",
                version = "1.6.0",
                description = "User CRUD service built with hexagonal architecture, Spring Boot 3 and MongoDB. " +
                              "Authenticate via POST /api/auth/login to receive a JWT bearer token, " +
                              "then click 'Authorize' and enter: Bearer <token>"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token obtained from POST /api/auth/login"
)
@Configuration
public class OpenApiConfig {
}
