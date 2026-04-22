# hexagonal-demo

> A production-shaped **RESTful User management service** built with **Spring Boot 3** and **MongoDB**, demonstrating **Hexagonal Architecture** (Ports & Adapters) — clean, testable, and ready to evolve.

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=flat-square&logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=flat-square&logo=mongodb)
![Testcontainers](https://img.shields.io/badge/Testcontainers-1.20.1-2496ED?style=flat-square&logo=docker)
![OpenAPI](https://img.shields.io/badge/OpenAPI-3-85EA2D?style=flat-square&logo=swagger)
![Build](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Domain Model](#domain-model)
- [API Reference](#api-reference)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Documentation](#documentation)
- [Tech Stack](#tech-stack)

---

## Overview

This project showcases how to structure a **Spring Boot microservice** following the **Hexagonal Architecture** pattern (also known as *Ports & Adapters*, introduced by Alistair Cockburn).

The core principle: **the business logic must never depend on infrastructure**. HTTP, MongoDB, and Docker are plug-in details — the domain and application layers remain pure, framework-free, and independently testable.

Key features implemented:

- ✅ Full **CRUD REST API** for a `User` resource
- ✅ **Pagination and dynamic filtering** (name, age range) on list endpoint
- ✅ **Input validation** with detailed field-level error responses
- ✅ **OpenAPI 3 / Swagger UI** auto-generated from annotations
- ✅ **Unit tests** (Mockito — zero infrastructure, milliseconds)
- ✅ **Integration tests** (Testcontainers — real MongoDB in Docker)
- ✅ **Docker Compose** stack for one-command local deployment
- ✅ **Postman collection** ready to import and run

---

## Architecture

The application is divided into three concentric rings. Dependencies always point **inward** — outer rings know about inner rings, never the reverse.

```
┌─────────────────────────────────────────────────────────────────┐
│  ADAPTERS  (outer ring)                                         │
│                                                                 │
│   REST Controller ──►  ┌───────────────────────────────────┐   │
│   DTOs / Mappers        │  APPLICATION  (middle ring)       │   │
│                         │                                   │   │
│                         │  Inbound Ports  (use-case ifaces) │   │
│                         │  Outbound Port  (persistence iface│   │
│                         │  UserService    (orchestration)   │   │
│                         │                                   │   │
│                         │  ┌─────────────────────────────┐  │   │
│                         │  │  DOMAIN  (inner ring)       │  │   │
│   MongoDB Adapter  ──►  │  │  User · UserPage            │  │   │
│   Spring Data           │  │  Pure Java — no frameworks  │  │   │
│                         │  └─────────────────────────────┘  │   │
│                         └───────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

| Ring | Responsibility | Framework imports |
|------|---------------|-------------------|
| **Domain** | Business entities and value objects | None |
| **Application** | Use-case orchestration, port interfaces | Spring `@Service` only |
| **Adapters** | HTTP translation, MongoDB persistence | Spring MVC, Spring Data |

---

## Project Structure

```
src/main/java/com/example/
│
├── config/
│   └── OpenApiConfig.java              # OpenAPI / Swagger definition
│
└── user/
    ├── domain/
    │   └── model/
    │       ├── User.java               # Core entity (Java record)
    │       └── UserPage.java           # Pagination value object
    │
    ├── application/
    │   ├── port/
    │   │   ├── in/                     # Inbound ports (use-case contracts)
    │   │   │   ├── CreateUserUseCase.java
    │   │   │   ├── GetUserUseCase.java
    │   │   │   ├── ListUsersUseCase.java
    │   │   │   ├── UpdateUserUseCase.java
    │   │   │   └── DeleteUserUseCase.java
    │   │   └── out/                    # Outbound port (persistence contract)
    │   │       └── UserPersistencePort.java
    │   ├── service/
    │   │   └── UserService.java        # Implements all use cases
    │   └── exception/
    │       └── UserNotFoundException.java
    │
    └── adapters/
        ├── in/rest/                    # Inbound adapter — HTTP / REST
        │   ├── UserController.java
        │   ├── UserRestMapper.java
        │   ├── RestExceptionHandler.java
        │   └── dto/
        │       ├── CreateUserRequest.java
        │       ├── UpdateUserRequest.java
        │       ├── UserResponse.java
        │       └── UserPageResponse.java
        └── out/mongo/                  # Outbound adapter — MongoDB
            ├── UserMongoPersistenceAdapter.java
            ├── SpringDataUserRepository.java
            ├── UserDocument.java
            └── UserMongoMapper.java
```

---

## Domain Model

```java
public record User(String id, String name, String address, Integer age) {}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `id` | `String` | MongoDB ObjectId (auto-generated) |
| `name` | `String` | Required, non-blank |
| `address` | `String` | Required, non-blank |
| `age` | `Integer` | Required, 0–150 |

---

## API Reference

### Endpoints

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| `POST` | `/api/users` | Create a new user | `201 Created` |
| `GET` | `/api/users/{id}` | Retrieve a user by ID | `200` / `404` |
| `GET` | `/api/users` | List users — paginated & filtered | `200` |
| `PUT` | `/api/users/{id}` | Full update of an existing user | `200` / `404` |
| `DELETE` | `/api/users/{id}` | Delete a user | `204` / `404` |

### List Users — Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `name` | `string` | — | Case-insensitive partial name match |
| `minAge` | `integer` | — | Minimum age, inclusive |
| `maxAge` | `integer` | — | Maximum age, inclusive |
| `page` | `integer` | `0` | Zero-based page index |
| `size` | `integer` | `20` | Number of results per page |

### Example — Create User

```http
POST /api/users
Content-Type: application/json

{
  "name": "Alice",
  "address": "Berlin",
  "age": 30
}
```

```json
HTTP/1.1 201 Created

{
  "id": "6628f3b2e4b0c914a5d1e001",
  "name": "Alice",
  "address": "Berlin",
  "age": 30
}
```

### Example — List with Filters

```http
GET /api/users?name=ali&minAge=25&maxAge=40&page=0&size=10
```

```json
{
  "content": [ { "id": "...", "name": "Alice", "address": "Berlin", "age": 30 } ],
  "totalElements": 1,
  "totalPages": 1,
  "page": 0,
  "size": 10
}
```

### Error Response (Validation)

```json
HTTP/1.1 400 Bad Request

{
  "name": "name is required",
  "age": "age must be at least 0"
}
```

---

## Getting Started

### Prerequisites

| Tool | Minimum version |
|------|----------------|
| Java | 17 |
| Maven | 3.9 |
| Docker | 24 (for Compose & integration tests) |

### Run locally (MongoDB required separately)

```bash
mvn spring-boot:run
```

### Run the full stack with Docker Compose

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| REST API | http://localhost:8111 |
| Swagger UI | http://localhost:8111/swagger-ui.html |
| MongoDB | localhost:27017 |

### Configuration

Environment variables override `application.yml` at runtime:

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGODB_URI` | `mongodb://localhost:27017/hexagonal_demo` | MongoDB connection string |
| `SERVER_PORT` | `8111` | HTTP server port |

---

## Running Tests

```bash
# All tests — unit + integration (requires Docker)
mvn test

# Unit tests only — no Docker required, runs in < 1 second
mvn test -Dtest=UserServiceTest

# Integration tests only — spins up real MongoDB via Testcontainers
mvn test -Dtest=UserControllerIntegrationTest
```

### Test coverage

| Suite | Technology | Scope | Speed |
|-------|-----------|-------|-------|
| `UserServiceTest` | JUnit 5 + Mockito | Business logic in isolation | ~10 ms |
| `UserControllerIntegrationTest` | Testcontainers + MockMvc | Full stack end-to-end | ~20 s (first run) |

The integration test suite covers: create, read, update, delete, name/age filtering, pagination, and validation error handling — all against a real MongoDB 7 instance running inside Docker.

---

## Documentation

| Document | Description |
|----------|-------------|
| [`docs/hexagonal-architecture.md`](docs/hexagonal-architecture.md) | Deep-dive into the architecture pattern, dependency flow, and design decisions |
| [`docs/testcontainers.md`](docs/testcontainers.md) | How Testcontainers works, lifecycle, and test isolation strategy |
| [`docs/presentation.html`](docs/presentation.html) | Interactive slide deck for team demos (open in any browser) |
| [`postman/User-CRUD.postman_collection.json`](postman/User-CRUD.postman_collection.json) | Ready-to-import Postman collection with all CRUD requests |

---

## Tech Stack

| Technology | Version | Role |
|-----------|---------|------|
| Java | 17 LTS | Language |
| Spring Boot | 3.3.4 | Application framework |
| Spring Data MongoDB | — | MongoDB integration |
| Spring Validation | — | Bean Validation (JSR-380) |
| MongoDB | 7 | Primary datastore |
| springdoc-openapi | 2.6.0 | OpenAPI 3 / Swagger UI generation |
| JUnit 5 | — | Test framework |
| Mockito | — | Mocking for unit tests |
| Testcontainers | 1.20.1 | Real Docker containers in integration tests |
| Docker Compose | — | Local multi-service orchestration |
