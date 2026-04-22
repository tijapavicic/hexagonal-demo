# Hexagonal Architecture — How It Works in This Project

## What Is Hexagonal Architecture?

Hexagonal Architecture (also known as **Ports & Adapters**) was introduced by Alistair Cockburn in
2005. The central idea is simple:

> **The application core (business logic) must not depend on any infrastructure detail.**
> Infrastructure depends on the core — never the other way around.

This means your domain and use-case code knows nothing about HTTP, MongoDB, message queues, or any
external system. Those details live in interchangeable adapters on the outside.

The name "hexagonal" is just a visual metaphor — the shape has no special meaning. The hex has
multiple sides to suggest there are many possible entry/exit points (REST, CLI, message broker,
database, etc.).

```
         ┌──────────────────────────────────────────┐
         │                                          │
  REST   │   ┌──────────────────────────────────┐  │   MongoDB
 ──────► │   │                                  │  │ ◄────────
         │   │        APPLICATION CORE          │  │
  CLI    │   │   (Domain + Use Cases/Services)  │  │   File System
 ──────► │   │                                  │  │ ◄────────
         │   └──────────────────────────────────┘  │
         │                                          │
         └──────────────────────────────────────────┘
              ▲                          ▲
         Driving side               Driven side
       (initiates action)        (receives commands)
```

---

## The Three Layers

### 1. Domain (innermost)
Pure business concepts. No framework annotations, no Spring, no MongoDB. Just Java.

### 2. Application (use cases)
Orchestrates the domain to fulfil one business operation (e.g. "create a user").
Defines **Ports** — interfaces that describe what the application needs from or offers to the
outside world.

### 3. Adapters (outermost)
Concrete implementations that connect the application to a specific technology.
- **Inbound adapters** — translate external calls *into* use-case calls (e.g. REST controller)
- **Outbound adapters** — translate use-case calls *into* external system calls (e.g. MongoDB repository)

---

## Ports Explained

A **Port** is just a Java interface sitting in the application layer.

| Port type | Direction | Defined in | Implemented in |
|-----------|-----------|-----------|----------------|
| **Inbound port** (driving) | outside → app | `application/port/in` | `application/service` |
| **Outbound port** (driven) | app → outside | `application/port/out` | `adapters/out` |

The application core **depends on abstractions (ports), not on implementations**.
This is the Dependency Inversion Principle applied architecturally.

---

## Project Package Structure

```
com.example
│
├── config/                          # Infrastructure configuration (OpenAPI, etc.)
│
└── user/
    │
    ├── domain/                      ← INNER RING
    │   └── model/
    │       ├── User.java            # Core business entity (record)
    │       └── UserPage.java        # Pagination value object
    │
    ├── application/                 ← MIDDLE RING
    │   ├── port/
    │   │   ├── in/                  # Inbound ports (use-case interfaces)
    │   │   │   ├── CreateUserUseCase.java
    │   │   │   ├── GetUserUseCase.java
    │   │   │   ├── ListUsersUseCase.java
    │   │   │   ├── UpdateUserUseCase.java
    │   │   │   └── DeleteUserUseCase.java
    │   │   └── out/                 # Outbound port (persistence interface)
    │   │       └── UserPersistencePort.java
    │   ├── service/
    │   │   └── UserService.java     # Implements all inbound ports, uses outbound port
    │   └── exception/
    │       └── UserNotFoundException.java
    │
    └── adapters/                    ← OUTER RING
        ├── in/
        │   └── rest/                # Inbound adapter — HTTP/REST
        │       ├── UserController.java
        │       ├── UserRestMapper.java
        │       ├── RestExceptionHandler.java
        │       └── dto/
        │           ├── CreateUserRequest.java
        │           ├── UpdateUserRequest.java
        │           ├── UserResponse.java
        │           └── UserPageResponse.java
        └── out/
            └── mongo/               # Outbound adapter — MongoDB
                ├── UserMongoPersistenceAdapter.java
                ├── SpringDataUserRepository.java
                ├── UserDocument.java
                └── UserMongoMapper.java
```

---

## Dependency Flow (The Golden Rule)

```
adapters/in  ──►  application/port/in  ──►  application/service
                                                    │
                                                    ▼
                                        application/port/out
                                                    ▲
                                                    │
adapters/out  ──────────────────────────────────────┘
```

Arrows show the **direction of dependencies** (what knows about what).

- `UserController` knows about `CreateUserUseCase` (an interface) — it does **not** know about
  `UserService` directly.
- `UserService` knows about `UserPersistencePort` (an interface) — it does **not** know about
  `UserMongoPersistenceAdapter` or MongoDB.
- Spring's dependency injection wires the concrete implementations at runtime.

---

## Each Layer in Code

### Domain — `User.java`

```java
// Pure Java. No Spring. No annotations. No imports from outside the domain.
public record User(String id, String name, String address, Integer age) {
}
```

The domain knows nothing about how it is stored or how it is delivered.

---

### Inbound Port — `CreateUserUseCase.java`

```java
// An interface in the application layer.
// The REST adapter and the service both depend on this contract.
public interface CreateUserUseCase {
    User create(User user);
}
```

---

### Application Service — `UserService.java`

```java
@Service
public class UserService implements CreateUserUseCase, /* ... other use cases */ {

    // Depends on the outbound PORT (interface), not on MongoDB
    private final UserPersistencePort persistencePort;

    @Override
    public User create(User user) {
        // Pure business logic — strip client-supplied id, let the DB generate one
        return persistencePort.save(new User(null, user.name(), user.address(), user.age()));
    }
}
```

`UserService` is the heart of the application. It is a plain Spring `@Service` but has:
- **No HTTP knowledge** (no `HttpServletRequest`, no `@RequestBody`)
- **No MongoDB knowledge** (no `MongoTemplate`, no `@Document`)

---

### Outbound Port — `UserPersistencePort.java`

```java
// Defined in the application layer.
// UserService calls this. MongoDB adapter implements this.
public interface UserPersistencePort {
    User save(User user);
    Optional<User> findById(String id);
    UserPage findAll(String name, Integer minAge, Integer maxAge, int page, int size);
    void deleteById(String id);
}
```

Notice the method signatures use **domain objects** (`User`, `UserPage`), not MongoDB documents.

---

### Inbound Adapter — `UserController.java`

```java
@RestController               // ← HTTP concern lives here, not in the service
@RequestMapping("/api/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;  // ← depends on interface

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        // 1. Map HTTP DTO → domain object
        // 2. Call the use case
        // 3. Map domain object → HTTP response DTO
        return mapper.toResponse(createUserUseCase.create(mapper.toDomain(request)));
    }
}
```

The controller's only job is **translation**: HTTP ↔ domain. No business logic lives here.

---

### Outbound Adapter — `UserMongoPersistenceAdapter.java`

```java
@Component          // ← MongoDB concern lives here, not in the service
public class UserMongoPersistenceAdapter implements UserPersistencePort {

    private final MongoTemplate mongoTemplate;

    @Override
    public UserPage findAll(String name, Integer minAge, Integer maxAge, int page, int size) {
        // Build a dynamic Criteria query — MongoDB detail invisible to the service
        Query query = buildQuery(name, minAge, maxAge);
        // ...
    }
}
```

The adapter's only job is **translation**: domain calls ↔ MongoDB queries.

---

## Why Does This Matter?

### Swap the database without touching business logic

Want to migrate from MongoDB to PostgreSQL?

1. Write a new `UserJpaPersistenceAdapter implements UserPersistencePort`
2. Remove the Mongo adapter (or keep both behind a feature flag)
3. `UserService` is **completely unchanged**

### Test business logic without infrastructure

`UserServiceTest` tests all business rules — validation, not-found exceptions, ID stripping — with
**zero Spring context and zero Docker**. It uses a Mockito mock for `UserPersistencePort` and runs
in milliseconds.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserPersistencePort persistencePort;   // ← fake, no real DB

    // Test runs in ~10ms. Pure Java.
}
```

### Swap the delivery mechanism

Need a CLI tool or a Kafka consumer that also creates users? Add a new inbound adapter:

```java
@Component
public class UserKafkaConsumer {

    private final CreateUserUseCase createUserUseCase;  // ← same use case

    @KafkaListener(topics = "user-events")
    public void onMessage(CreateUserRequest event) {
        createUserUseCase.create(...);
    }
}
```

No change to `UserService` or the domain.

---

## Hexagonal vs Layered Architecture

| | Traditional Layered | Hexagonal |
|-|---------------------|-----------|
| Dependency direction | Top → Bottom (Controller → Service → Repository) | Always toward the domain core |
| Business logic location | Service layer (but leaks into controllers/repos) | Strictly isolated in application layer |
| Testing | Hard to unit-test — DB/HTTP tightly coupled | Domain/service fully unit-testable |
| Database swap | Ripples through all layers | Replace one adapter only |
| New delivery channel | Modify existing service | Add a new inbound adapter only |

---

## Summary

```
  ┌─────────────────────────────────────────────────────────┐
  │  ADAPTERS (outer)                                       │
  │                                                         │
  │   REST Controller ──►  ┌─────────────────────────┐     │
  │                        │  APPLICATION (middle)   │     │
  │                        │                         │     │
  │                        │  Ports (interfaces)     │     │
  │                        │       +                 │     │
  │                        │  Service (use cases)    │     │
  │                        │       +                 │     │
  │                        │  ┌───────────────────┐  │     │
  │                        │  │  DOMAIN (inner)   │  │     │
  │                        │  │  User, UserPage   │  │     │
  │                        │  └───────────────────┘  │     │
  │                        └─────────────────────────┘     │
  │   MongoDB Adapter ──►              ▲                    │
  │                                    │                    │
  │                   implements UserPersistencePort        │
  └─────────────────────────────────────────────────────────┘
```

| Layer | Contains | Depends on |
|-------|----------|-----------|
| Domain | `User`, `UserPage` | Nothing |
| Application | Ports, `UserService`, exceptions | Domain only |
| Adapters | Controllers, Mongo adapter, DTOs | Application ports + frameworks |

The domain is the most stable, most tested, and most reusable part of the system.
Frameworks and databases are **details** — they plug in from the outside.

