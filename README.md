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

## API endpoints

- `POST /api/users`
- `GET /api/users/{id}`
- `GET /api/users`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`

## Tests

```bash
mvn test
```

## Postman

Import `postman/User-CRUD.postman_collection.json`.

