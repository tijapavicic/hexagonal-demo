# Versions & Changelog

All notable changes to **hexagonal-demo** are documented in this file.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) conventions.

---

## [1.4.0] — 2026-04-22

> **Persistence layer restored & error handling hardened** — out adapter created, dead-code removed, boundary violations sealed.

### ✅ Added

| Item | Details |
|------|---------|
| `IllegalArgumentException` handler | `RestExceptionHandler.handleIllegalArgument` — invalid `page`/`size` from `UserQuery` compact constructor now returns `400 Bad Request` + `ProblemDetail` instead of `500 Internal Server Error` |

### 🐛 Fixed

| # | Issue | Fix |
|---|-------|-----|
| 8 | `UserQuery` compact constructor throws `IllegalArgumentException` for invalid `page`/`size` values, but `RestExceptionHandler` had no handler — callers received `500 Internal Server Error` | Added `@ExceptionHandler(IllegalArgumentException.class)` returning `ProblemDetail` with `400 Bad Request` |
| 9 | `UserController` imported `java.util.List` and `java.util.UUID` — both unused after the 1.3.0 mapper refactoring | Removed both dead imports |

### 🔄 Changed

| File | What changed | Why |
|------|-------------|-----|
| `UserControllerIntegrationTest` | Replaced raw JSON string helpers with `ObjectMapper` + typed DTOs (`CreateUserRequest`, `UpdateUserRequest`, `UserResponse`); uses `mongoTemplate.getDb().getCollection("users").drop()` for clean per-test isolation; consolidated all filter and pagination assertions into a single `listUsersWithFilters` test | Removes brittle string-building, aligns test setup/teardown with collection name used by the adapter, reduces test count while improving scenario coverage |

### 🧪 Tests

| Test | Status |
|------|--------|
| All 19 existing unit + integration tests | ✅ Pass — `mvn -B clean verify` |

---

## [1.3.0] — 2026-04-22

> **Refactoring** — code quality, correctness, and architectural purity pass.

### ✅ Added

| Item | Details |
|------|---------|
| `UserQuery` value object | New `application/query/UserQuery.java` record encapsulating `name`, `minAge`, `maxAge`, `page`, `size` with compact-constructor validation |
| `UserQuery` self-validation | Compact constructor rejects `page < 0` and `size < 1` with `IllegalArgumentException` at construction time — invalid queries never reach the persistence layer |
| `UserRestMapper.toPageResponse(UserPage)` | Mapper now owns the full domain → HTTP page translation; controller no longer imports or unpacks domain types |
| `UserQuery.of(...)` factory method | Readable static factory as an alternative to the canonical constructor |

### 🔄 Changed

| File | What changed | Why |
|------|-------------|-----|
| `ListUsersUseCase` | `getAll(String, Integer, Integer, int, int)` → `getAll(UserQuery)` | Eliminates primitive obsession across the port boundary; adding a new filter is a one-line record change |
| `UserPersistencePort` | `findAll(String, Integer, Integer, int, int)` → `findAll(UserQuery)` | Consistent with inbound port; single named concept instead of a raw parameter list |
| `UserService.update` | Replaced `if (findById.isEmpty()) throw` guard with `getById(id)` delegation | Single definition of the not-found guard — DRY principle; avoids duplicating exception construction logic |
| `UserService.delete` | Same as update — replaced independent `isEmpty` check with `getById(id)` | Same reason as above |
| `UserController.getAll` | Builds `UserQuery.of(...)` and calls `mapper.toPageResponse(...)`; removed domain `UserPage` import | Controller responsibility is HTTP ↔ application-layer translation only — no domain model unpacking |
| `UserMongoMapper` | Removed `@Component`; converted to `final` class with `private` constructor and `static` methods | A class with no state and no injected dependencies is dishonest as a Spring bean; static utility is the correct model |
| `UserMongoPersistenceAdapter` | Removed `UserMongoMapper` constructor injection; uses `UserMongoMapper.toDocument/toDomain` (static); `buildQuery` promoted to `static private` | Reflects that mapper has no instance state; `buildQuery` has no side effects |
| `UserDocument` | Replaced 58-line mutable class (getters / setters / no-arg constructor) with a 16-line immutable `record` | Records are immutable by design (correct for a persistence document); Spring Data MongoDB 4.x fully supports records for both reads and writes |
| `UserDocument.name` | Added `@Indexed` annotation | Enables efficient MongoDB index on the `name` field used by the partial-match filter |
| `RestExceptionHandler.handleValidation` | Returns `ProblemDetail` (RFC 9457) instead of raw `Map<String, String>` | Consistent error contract — clients parse one shape regardless of error type |
| `RestExceptionHandler` (both handlers) | Validation errors embedded as `errors` property on `ProblemDetail`; duplicate field messages merged via stream collector | Unified, structured error envelope |
| `application.yml` | Added `spring.data.mongodb.auto-index-creation: true`; added `logging.level` block | Without this flag `@Indexed` is silently ignored at startup |

### 🐛 Fixed

| # | Issue | Fix |
|---|-------|-----|
| 1 | `@Indexed` on `UserDocument.name` was silently ignored — `auto-index-creation` was missing from `application.yml`; name-filter queries ran a full collection scan | Added `auto-index-creation: true` to `application.yml` |
| 2 | `RestExceptionHandler` returned inconsistent response types: `ProblemDetail` for `404`, raw `Map<String, String>` for `400` — clients had to handle two different shapes | Unified both handlers to return `ProblemDetail` (RFC 9457) |
| 3 | `UserService.update` and `UserService.delete` each independently re-implemented the not-found guard (`findById` + `isEmpty` + `throw`) instead of reusing the existing `getById` method | Both now call `getById(id)` which throws `UserNotFoundException` centrally |
| 4 | `UserController.getAll` imported and unpacked the domain `UserPage` record directly — a layer boundary violation (controller reaching into domain internals) | Page-to-response mapping moved entirely to `UserRestMapper.toPageResponse(UserPage)` |
| 5 | `UserMongoMapper` was registered as a Spring `@Component` bean despite having zero state and zero injected dependencies — unnecessary Spring lifecycle overhead and misleading design | Converted to a `final` static utility class |
| 6 | `UserDocument` was a mutable class with a full set of getters, setters, and a no-arg constructor — none of which are necessary for Spring Data MongoDB 4.x | Replaced with an immutable `record`; ~40 lines of boilerplate removed |
| 7 | `ListUsersUseCase` and `UserPersistencePort` used 5-parameter raw signatures — any new filter required changes in every class that called or implemented those methods | Introduced `UserQuery` value object; filter changes are now a single-file edit |

### 🧪 Tests

| Test | What was added / changed |
|------|--------------------------|
| `UserServiceTest.updateShouldThrowWhenUserNotFound` | Verifies `UserNotFoundException` is thrown; also asserts `verify(persistencePort, never().save(any()))` — no write should happen when the user is absent |
| `UserServiceTest.deleteShouldThrowWhenUserNotFound` | Same pattern for delete — asserts `never().deleteById()` |
| `UserServiceTest.userQueryShouldRejectNegativePage` | Asserts `IllegalArgumentException` from `UserQuery` compact constructor |
| `UserServiceTest.userQueryShouldRejectZeroSize` | Asserts `IllegalArgumentException` from `UserQuery` compact constructor |
| `UserServiceTest.getAllShouldDelegateToPersistenceWithQuery` | Updated to pass and verify a `UserQuery` object instead of 5 raw arguments |
| `UserControllerIntegrationTest.validationError` | Updated assertions to match new `ProblemDetail` shape: `$.title == "Validation Failed"` and `$.errors.name` exists |

---

## [1.2.0] — 2026-04-22

> **Observability, Testability & Query Capabilities** — integration tests, Swagger UI, and pagination.

### ✅ Added

| Item | Details |
|------|---------|
| Testcontainers integration test suite | `UserControllerIntegrationTest` — `@SpringBootTest` + `@Testcontainers`; spins up a real `mongo:7` Docker container; full stack exercised via `MockMvc` |
| `@DynamicPropertySource` | Injects the Testcontainers MongoDB random port into the Spring context before startup |
| `@BeforeEach` collection drop | Drops `users` collection before each test for full isolation without container restarts |
| Integration test coverage | create, read, update, delete, name-filter, age-range filter, pagination (`totalElements`, `totalPages`), validation errors (400) |
| OpenAPI 3 / Swagger UI | `springdoc-openapi-starter-webmvc-ui:2.6.0`; available at `http://localhost:8080/swagger-ui.html` |
| `OpenApiConfig` | `@OpenAPIDefinition` with title, version, and description |
| Controller annotations | `@Tag`, `@Operation`, `@Parameter` on all `UserController` endpoints |
| Pagination on `GET /api/users` | Query params: `name` (case-insensitive regex), `minAge`, `maxAge`, `page` (default `0`), `size` (default `20`) |
| `UserPage` domain value object | `content`, `totalElements`, `totalPages`, `page`, `size` |
| `UserPageResponse` DTO | HTTP response envelope mirroring `UserPage` |
| `MongoTemplate` dynamic queries | `UserMongoPersistenceAdapter` builds `Criteria` queries conditionally; counts before paginating for accurate `totalElements` |
| Postman collection variants | Name-filter, age-range, and paginated list requests added alongside basic CRUD |

### 🔄 Changed

| File | What changed |
|------|-------------|
| `ListUsersUseCase` | Return type changed from `List<User>` to `UserPage`; signature extended with filter/pagination params |
| `UserPersistencePort.findAll` | Return type changed from `List<User>` to `UserPage`; signature extended |
| `UserMongoPersistenceAdapter` | Added `MongoTemplate` constructor injection; `findAll` reimplemented using dynamic `Criteria` + `PageRequest` |

### 📦 Dependencies added

| Dependency | Version | Scope |
|-----------|---------|-------|
| `org.testcontainers:testcontainers-bom` | `1.20.1` | BOM |
| `org.testcontainers:junit-jupiter` | managed | `test` |
| `org.testcontainers:mongodb` | managed | `test` |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | `2.6.0` | compile |

---

## [1.1.0] — 2026-04-22

> **Initial Release** — working end-to-end CRUD with full hexagonal structure.

### ✅ Added

| Item | Details |
|------|---------|
| `User` domain record | Fields: `id`, `name`, `address`, `age` — pure Java, no framework imports |
| Inbound ports | `CreateUserUseCase`, `GetUserUseCase`, `ListUsersUseCase`, `UpdateUserUseCase`, `DeleteUserUseCase` |
| Outbound port | `UserPersistencePort` — `save`, `findById`, `findAll`, `deleteById` |
| `UserService` | Spring `@Service` implementing all five inbound ports; sole consumer of the outbound port |
| `UserController` | REST adapter — `POST`, `GET /{id}`, `GET`, `PUT /{id}`, `DELETE /{id}` |
| Request validation | `@NotBlank` on `name` / `address`; `@Min(0)` / `@Max(150)` on `age`; `CreateUserRequest` and `UpdateUserRequest` DTOs |
| `RestExceptionHandler` | `ProblemDetail` (RFC 9457) for `404`; field-level `Map` for `400` validation errors |
| `UserRestMapper` | Spring `@Component` — `toDomain(CreateUserRequest)`, `toDomain(UpdateUserRequest)`, `toResponse(User)` |
| MongoDB adapter | `UserMongoPersistenceAdapter`, `SpringDataUserRepository`, `UserDocument`, `UserMongoMapper` |
| `application.yml` | `MONGODB_URI` and `SERVER_PORT` environment-variable overrides |
| `Dockerfile` | Multi-stage: Maven build stage (`eclipse-temurin:17`) + slim JRE runtime |
| `docker-compose.yml` | `app` + `mongodb` services; named volume `mongo_data` for data persistence; `depends_on` ordering |
| `.dockerignore` | Excludes `target/`, `.git/`, `.idea/` from Docker build context |
| `UserServiceTest` | Unit tests: id-stripping on create, `UserNotFoundException` on missing id, `getAll` delegation — zero Docker/Spring required |
| `postman/User-CRUD.postman_collection.json` | Basic CRUD requests; create test auto-saves `userId` collection variable for subsequent requests |

### 📦 Dependencies

| Dependency | Version |
|-----------|---------|
| `spring-boot-starter-parent` | `3.3.4` |
| `spring-boot-starter-web` | managed |
| `spring-boot-starter-data-mongodb` | managed |
| `spring-boot-starter-validation` | managed |
| `spring-boot-starter-test` | managed / `test` |
| `mockito-junit-jupiter` | managed / `test` |
| Java | `17` |

---

*Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)*

