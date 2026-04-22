# Hexagonal Demo (Spring Boot + MongoDB)

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



