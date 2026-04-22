# Hexagonal Demo (Spring Boot + MongoDB)

![Version](https://img.shields.io/badge/version-1.3.0-blue?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=flat-square&logo=mongodb)
![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)

> See [VERSIONS.md](VERSIONS.md) for the full changelog and list of fixed issues per release.

User CRUD backend using a hexagonal architecture style.

## Domain model

- `id`
- `name`
- `address`
- `age`

## Run locally

```bash
mvn spring-boot:run
```

## Run with Docker Compose

```bash
docker compose up --build
```

App URL: `http://localhost:8080`

## Swagger UI

`http://localhost:8080/swagger-ui.html`

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/users` | Create user |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/api/users` | List users (paginated + filtered) |
| `PUT` | `/api/users/{id}` | Update user |
| `DELETE` | `/api/users/{id}` | Delete user |

### List users query params

| Param | Type | Description |
|-------|------|-------------|
| `name` | string | Partial, case-insensitive name filter |
| `minAge` | int | Minimum age (inclusive) |
| `maxAge` | int | Maximum age (inclusive) |
| `page` | int | Zero-based page number (default: `0`) |
| `size` | int | Page size (default: `20`) |

### Example response for list

```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```

## Tests

```bash
mvn test
```

- **Unit tests** — `UserServiceTest` (no external dependencies)
- **Integration tests** — `UserControllerIntegrationTest` (spins up MongoDB via Testcontainers, requires Docker)

## Postman

Import `postman/User-CRUD.postman_collection.json`.



