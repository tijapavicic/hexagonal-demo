# Hexagonal Demo

![Version](https://img.shields.io/badge/version-1.5.0-blue?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=flat-square&logo=mongodb)
![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)

Spring Boot + MongoDB sample service for user management, organized with a hexagonal architecture. The project is intentionally small, but it includes the pieces you would expect in a production-shaped service: layered boundaries, JWT-based auth, validation, pagination/filtering, OpenAPI docs, and automated tests.

> See [VERSIONS.md](VERSIONS.md) for release history and [README_DETAILED.md](README_DETAILED.md) for a longer walkthrough.

## What This Project Shows

- Hexagonal architecture with clear separation between domain, application, and adapters
- REST CRUD API for `User`
- MongoDB persistence
- JWT login endpoint plus bearer-token protection for `/api/users/**`
- Filtering and pagination for list endpoints
- Validation and structured error handling
- Unit tests, architecture tests, and Docker-backed integration tests

## Stack

- Java 17
- Spring Boot 3.3.4
- MongoDB 7
- Maven
- Testcontainers
- Springdoc / Swagger UI

## Project Layout

```text
src/main/java/com/example/
├── config/                  # security, JWT, OpenAPI
├── user/domain/             # pure domain model and domain exceptions
├── user/application/        # use cases, ports, application services
└── user/adapters/           # REST input and MongoDB output adapters
```

## Domain Model

The API manages a `User` with these fields:

- `id`
- `name`
- `address`
- `age`

Domain validation lives in the domain model, so invalid users cannot be created regardless of which adapter calls into the application layer.

## Quick Start

### Prerequisites

- Java 17
- Maven 3.9+
- Docker if you want Docker Compose or integration tests

### Run Locally

The local profile provides developer-friendly defaults for the JWT secret and login credentials.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

App URL: `http://localhost:8111`

Swagger UI: `http://localhost:8111/swagger-ui.html`

### Run With Docker Compose

```bash
docker compose up --build
```

This starts:

- the Spring Boot app on `http://localhost:8111`
- MongoDB on `localhost:27017`
- Keycloak on `http://localhost:8180`

## Authentication

`POST /api/auth/login` is public. All `/api/users/**` endpoints require a bearer token.

Local profile credentials:

- username: `admin`
- password: `admin123`

If you need your own local password hash:

```bash
htpasswd -bnBC 12 "" my-password | tr -d ':\n'
```

Example login:

```bash
curl -X POST http://localhost:8111/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"your-password"}'
```

With the repo defaults, replace `your-password` with `admin123`.

Example authenticated request:

```bash
curl http://localhost:8111/api/users \
  -H 'Authorization: Bearer <token>'
```

## Configuration

For non-local profiles, provide these environment variables:

| Variable | Required | Purpose |
|----------|----------|---------|
| `APP_JWT_SECRET` | Yes | HMAC signing secret for locally issued JWTs |
| `APP_USERNAME` | Yes | Login username for `POST /api/auth/login` |
| `APP_PASSWORD_HASH` | Yes | BCrypt password hash for the login user |
| `MONGODB_URI` | No | MongoDB connection string |
| `SERVER_PORT` | No | HTTP port, default `8111` |
| `KEYCLOAK_ISSUER_URI` | No | Issuer for Keycloak-issued bearer tokens |

Notes:

- `APP_JWT_SECRET` should be at least 32 characters.
- The local profile is intentionally more convenient; production-style profiles are intentionally stricter.

## API Summary

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/auth/login` | Obtain a bearer token |
| `POST` | `/api/users` | Create user |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/api/users` | List users with filters and pagination |
| `PUT` | `/api/users/{id}` | Update user |
| `DELETE` | `/api/users/{id}` | Delete user |

### List Users Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | string | - | Case-insensitive partial match |
| `minAge` | int | - | Minimum age, inclusive |
| `maxAge` | int | - | Maximum age, inclusive |
| `page` | int | `0` | Zero-based page number |
| `size` | int | `20` | Page size |

Example:

```bash
curl 'http://localhost:8111/api/users?name=ali&minAge=25&maxAge=40&page=0&size=10' \
  -H 'Authorization: Bearer <token>'
```

Example response:

```json
{
  "content": [
    {
      "id": "6628f3b2e4b0c914a5d1e001",
      "name": "Alice",
      "address": "Berlin",
      "age": 30
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 10
}
```

## Testing

Run the full test suite with:

```bash
mvn test
```

Test coverage includes:

- `UserServiceTest`: unit tests for application logic
- `ArchitectureTest`: ArchUnit rules to enforce the layering
- `UserControllerIntegrationTest`: integration tests using MongoDB via Testcontainers

Integration test behavior:

- If Docker is available, the Testcontainers-backed integration tests run.
- If Docker is not available, those integration tests are skipped and the rest of the suite still runs.

## Postman

Import [postman/User-CRUD.postman_collection.json](postman/User-CRUD.postman_collection.json).

## Related Docs

- [README_DETAILED.md](README_DETAILED.md)
- [docs/hexagonal-architecture.md](docs/hexagonal-architecture.md)
- [docs/important_considerations.md](docs/important_considerations.md)
- [docs/testcontainers.md](docs/testcontainers.md)
