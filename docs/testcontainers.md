# Testcontainers — How It Works in This Project

## What Is Testcontainers?

[Testcontainers](https://testcontainers.com/) is a Java library that spins up real Docker containers
**programmatically during your test run** and tears them down automatically when the tests finish.
This lets you test against a real MongoDB instance instead of mocking or using an embedded fake,
giving you much higher confidence that your code works in production.

---

## Dependencies Added (`pom.xml`)

```xml
<!-- Testcontainers BOM — manages all module versions together -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>1.20.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- JUnit 5 integration — lifecycle management via annotations -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- MongoDB module — pre-configured MongoDBContainer image -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Integration Test Walkthrough

File: `src/test/java/com/example/user/adapters/in/rest/UserControllerIntegrationTest.java`

### 1. `@SpringBootTest` + `@AutoConfigureMockMvc`

```java
@SpringBootTest
@AutoConfigureMockMvc
```

- `@SpringBootTest` boots the **full application context** (all beans, Spring Data, web layer).
- `@AutoConfigureMockMvc` injects a `MockMvc` instance that lets you make HTTP calls without
  starting a real HTTP server — fast, but exercises all filters, controllers, and serialization.

---

### 2. `@Testcontainers`

```java
@Testcontainers
class UserControllerIntegrationTest {
```

Activates Testcontainers JUnit 5 extension. It scans the class for `@Container`-annotated fields
and manages their lifecycle automatically (start before tests, stop after).

---

### 3. `@Container` — declaring the MongoDB container

```java
@Container
static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");
```

| Detail | Explanation |
|--------|-------------|
| `static` | A **static** field means the container is started **once per test class** and reused across all test methods — much faster than restarting for every test. |
| `MongoDBContainer` | Testcontainers module that wraps the official `mongo` Docker image and knows how to detect when MongoDB is ready to accept connections. |
| `"mongo:7"` | The Docker image tag to pull. Pinning the major version ensures reproducibility. |

---

### 4. `@DynamicPropertySource` — wiring the container into Spring

```java
@DynamicPropertySource
static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
}
```

This is the **glue** between Testcontainers and Spring Boot.

- The container binds MongoDB to a **random free port** on `localhost` (e.g. `27017` might already
  be in use on your machine — Testcontainers avoids conflicts this way).
- `getReplicaSetUrl()` returns the dynamic connection string, e.g.:
  `mongodb://localhost:49512/test`
- `@DynamicPropertySource` overrides `spring.data.mongodb.uri` **before** the application context
  starts, so Spring Data MongoDB connects to the container instead of the URL in
  `application.yml`.

---

### 5. `@BeforeEach` — test isolation

```java
@BeforeEach
void cleanUp() {
    mongoTemplate.getDb().getCollection("users").drop();
}
```

Since the container is **shared** across test methods (static field), data written by one test
would pollute the next. Dropping the `users` collection before each test ensures every test starts
from a clean slate without the overhead of restarting MongoDB.

---

### 6. What Each Test Covers

| Test | What It Verifies |
|------|-----------------|
| `createAndGetUser` | `POST /api/users` returns 201 + correct body; `GET /api/users/{id}` retrieves the same user |
| `updateUser` | `PUT /api/users/{id}` persists changed fields |
| `deleteUser` | `DELETE /api/users/{id}` returns 204; subsequent `GET` returns 404 |
| `listUsersWithFilters` | Name filter (regex, case-insensitive), age range filter, and pagination (`totalElements`, `totalPages`, `content` size) |
| `validationError` | Blank `name` field triggers Bean Validation → 400 response |

---

## Full Request Lifecycle (Integration Test)

```
Test Method
    │
    ▼
MockMvc.perform(post("/api/users") ...)
    │
    ▼
UserController          ← Spring MVC layer
    │
    ▼
UserService             ← application layer (pure Java)
    │
    ▼
UserMongoPersistenceAdapter  ← outbound adapter
    │
    ▼
MongoTemplate / SpringDataUserRepository
    │
    ▼
MongoDBContainer (real MongoDB in Docker)
```

This exercises **every layer** of the hexagonal architecture end-to-end.

---

## Comparison: Unit Test vs Integration Test

| | `UserServiceTest` | `UserControllerIntegrationTest` |
|-|-------------------|---------------------------------|
| Framework | JUnit 5 + Mockito | JUnit 5 + Spring Boot + Testcontainers |
| MongoDB | Mocked (`UserPersistencePort`) | Real MongoDB in Docker |
| Spring context | None (plain Java) | Full (`@SpringBootTest`) |
| Speed | Very fast (< 1s) | Slower (Docker pull + startup ~10–20s first run) |
| Confidence | Tests business logic in isolation | Tests the whole stack end-to-end |
| Docker required | No | Yes |

---

## Running the Tests

```bash
# All tests (unit + integration) — requires Docker running
mvn test

# Unit tests only — no Docker needed
mvn test -Dtest=UserServiceTest

# Integration tests only
mvn test -Dtest=UserControllerIntegrationTest
```

> **Tip:** On the first run Testcontainers will pull the `mongo:7` image from Docker Hub.
> Subsequent runs reuse the cached image and start in a few seconds.

---

## How Testcontainers Manages the Container Lifecycle

```
mvn test
  │
  ├─ JVM starts
  ├─ @Testcontainers extension detected
  ├─ @Container static field found → Docker container STARTED
  ├─ @DynamicPropertySource → Spring context started with container's port
  │
  ├─ @BeforeEach → collection dropped
  ├─ test 1 runs
  ├─ @BeforeEach → collection dropped
  ├─ test 2 runs
  │   ...
  │
  └─ All tests done → Docker container STOPPED and REMOVED automatically
```

The container is always cleaned up — even if a test throws an exception — because Testcontainers
registers a JVM shutdown hook.

