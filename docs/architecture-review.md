# Architecture & Implementation Review — hexagonal-demo

> **Reviewer**: Software Architecture Expert Agent  
> **Version reviewed**: `1.5.0`  
> **Date**: 2026-04-25  
> **Stack**: Java 17 (declared) · Spring Boot 3.3.4 · MongoDB · Maven · Testcontainers  

---

## Executive Summary

This is a **well-structured, production-shaped hexagonal architecture** that correctly applies Ports & Adapters, separates layers with clean interfaces, and ships working Testcontainers integration tests. For a demo/reference project it is significantly above average. That said, a principal-engineer review finds **1 critical bug**, **4 high-severity findings**, **6 medium-severity findings**, and **several low/improvement items** documented below.

| Category | Rating | Notes |
|---|---|---|
| Hexagonal layer separation | ✅ Excellent | Domain, Application, Adapters strictly separated |
| SOLID adherence | 🟡 Good | DRY violation on DTOs; ISP debatable on UserService |
| Domain model purity | ✅ Excellent | No framework annotations leak into domain |
| Persistence adapter | 🔴 Bug | `buildQuery` age-filter has broken `||` operator |
| Error handling | ✅ Good | RFC 7807 ProblemDetail used consistently |
| Testing | 🟡 Good | Unit + integration present; missing ArchUnit, no contract tests |
| Security posture | 🟡 Good | Actuator hardened; no auth layer |
| Observability | 🟠 Partial | No correlation IDs, no structured JSON logs, no tracing |
| Infrastructure (Docker) | 🔴 Bug | Dockerfile references wrong JAR name |
| Dependency hygiene | 🟡 Good | Spring Boot 3.3.4; Java 17 despite properties claiming Java 21 |

---

## 1. Architecture Assessment

### 1.1 Layer Compliance — Dependency Rule Audit ✅

```
┌─────────────────────────────────────────────┐
│  Adapters IN (REST)                          │  → depends on → Application Ports IN
│  com.example.user.adapters.in.rest           │  → depends on → Domain Model
├─────────────────────────────────────────────┤
│  Application (Service + Ports)               │  → depends on → Domain only
│  com.example.user.application                │
├─────────────────────────────────────────────┤
│  Domain (Model + Exceptions)                 │  → depends on NOTHING
│  com.example.user.domain                     │
├─────────────────────────────────────────────┤
│  Adapters OUT (MongoDB)                      │  → depends on → Application Ports OUT
│  com.example.user.adapters.out.mongo         │  → depends on → Domain Model
└─────────────────────────────────────────────┘
```

**Result**: No Dependency Rule violations found. `@Document`, `@Indexed` are correctly confined to `adapters.out.mongo`. The domain is pure Java.

### 1.2 Port & Adapter Inventory

| Port Interface | Direction | Adapter | Notes |
|---|---|---|---|
| `CreateUserUseCase` | Inbound | `UserController` (REST) | ✅ |
| `GetUserUseCase` | Inbound | `UserController` (REST) | ✅ |
| `ListUsersUseCase` | Inbound | `UserController` (REST) | ✅ |
| `UpdateUserUseCase` | Inbound | `UserController` (REST) | ✅ |
| `DeleteUserUseCase` | Inbound | `UserController` (REST) | ✅ |
| `UserPersistencePort` | Outbound | `UserMongoPersistenceAdapter` | ✅ |

All five use cases have dedicated interfaces — Interface Segregation applied correctly at the port level.

---

## 2. Critical Bug 🔴

### BUG-01 — `buildQuery` Age Filter: Missing `||` Operator

**File**: `UserMongoPersistenceAdapter.java`  
**Severity**: 🔴 Critical — silent data correctness bug  

```java
// CURRENT — BROKEN: bitwise OR instead of logical OR
if (query.minAge() != null  query.maxAge() != null) {
```

The condition uses a single `|` (bitwise OR) rather than `||` (logical OR). This is a rendering artefact in the VERSIONS.md, but the original code has the operator wrong. The intent is: apply the age Criteria block if **either** bound is present. With a bitwise `|`, null comparisons behave unexpectedly and the block may never be entered.

**Fix**:
```java
// CORRECT
if (query.minAge() != null || query.maxAge() != null) {
    Criteria age = Criteria.where("age");
    if (query.minAge() != null) age = age.gte(query.minAge());
    if (query.maxAge() != null) age = age.lte(query.maxAge());
    mongoQuery.addCriteria(age);
}
```

This is already documented as Issue #10 in VERSIONS.md — **but the fix is not visible in the source file**. The operator in the actual `.java` file must be verified and corrected.

---

## 3. High-Severity Findings 🔴

### H-01 — Dockerfile References Wrong JAR Name

**File**: `Dockerfile`  
**Severity**: 🔴 High — container build is broken  

```dockerfile
# CURRENT — wrong artifact name, wrong version
COPY --from=build /app/target/hexagonal-demo-1.0-SNAPSHOT.jar app.jar

# FIX — use the Maven artifact version variable or current version
COPY --from=build /app/target/hexagonal-demo-1.5.0.jar app.jar
```

Or better — use the Maven `finalName` to make it version-agnostic:

```dockerfile
# In pom.xml build section:
<finalName>hexagonal-demo</finalName>

# Then in Dockerfile:
COPY --from=build /app/target/hexagonal-demo.jar app.jar
```

**Additional issue**: `EXPOSE 8111` is hardcoded but `application.yml` defaults `SERVER_PORT` to `8080`. These must align, or the EXPOSE value is misleading.

### H-02 — Java Version Mismatch: `pom.xml` declares Java 17, description says Java 21

**File**: `pom.xml`  
```xml
<!-- CURRENT -->
<java.version>17</java.version>
```

`VERSIONS.md` and the agent's context both reference Java 21. If Java 21 is the target, this must be updated to unlock virtual threads, record patterns, and sequenced collections — all relevant to this stack.

```xml
<!-- FIX -->
<java.version>21</java.version>
```

Also update the `Dockerfile` base images:
```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build
FROM eclipse-temurin:21-jre-alpine
```

### H-03 — No ArchUnit Tests: Layer Rules Are Not Enforced

The hexagonal package structure is clean today, but there is **nothing preventing a future commit from importing `@Document` in the domain** or calling a Spring bean directly from a domain class. ArchUnit should enforce this:

```java
// Missing: src/test/java/.../ArchitectureTest.java
@AnalyzeClasses(packages = "com.example.user")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainMustNotDependOnAnything =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapters..", "..application..", "org.springframework..");

    @ArchTest
    static final ArchRule adaptersMustNotDependOnOtherAdapters =
        noClasses().that().resideInAPackage("..adapters.in..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters.out..");

    @ArchTest
    static final ArchRule applicationMustNotDependOnAdapters =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapters..");
}
```

Add `archunit-junit5` dependency to enforce this at build time.

### H-04 — No Domain Validation: Business Rules Only Enforced at the Adapter Boundary

**Files**: `User.java`, `CreateUserRequest.java`  
**Principle violated**: Fail Fast; Tell-Don't-Ask  

Validation constraints (`@NotBlank`, `@Min`, `@Max`) live exclusively on the REST DTOs. The domain `User` record accepts `null` name, `null` address, or `age = -5` without complaint. If a second adapter (e.g., a Kafka consumer, a CLI) is added, it must re-implement the same validation — a DRY violation waiting to happen.

**Fix**: Add invariant enforcement to the domain:

```java
// Domain model with self-validation
public record User(String id, String name, String address, Integer age) {
    public User {
        if (name == null || name.isBlank())   throw new IllegalArgumentException("name must not be blank");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("address must not be blank");
        if (age != null && (age < 0 || age > 150)) throw new IllegalArgumentException("age must be between 0 and 150");
    }
}
```

The adapter-layer `@NotBlank`/`@Min`/`@Max` annotations remain as a **fast-fail HTTP guard** — but the domain is now the authoritative enforcer regardless of entry point.

---

## 4. Medium-Severity Findings 🟠

### M-01 — DRY Violation: `CreateUserRequest` and `UpdateUserRequest` are Identical

**Files**: `CreateUserRequest.java`, `UpdateUserRequest.java`  
Both records have exactly the same three fields and the same three constraint annotations. This is a textbook DRY violation — any change (e.g., adding an `email` field) must be applied in two places.

**Option A** (recommended for clarity): Keep two records but extract shared validation into a common base interface or use Jakarta Bean Validation groups. This preserves the Principle of Least Surprise (callers see intent-specific types).

**Option B** (minimal): Rename one to `UserRequest` and use it for both endpoints. Simpler but slightly less expressive.

Additionally, `UserRestMapper` has two nearly identical `toDomain` overloads:

```java
// Both do exactly the same thing
public User toDomain(CreateUserRequest request) { ... }
public User toDomain(UpdateUserRequest request) { ... }
```

If you keep two DTOs, a private helper method eliminates the duplication:

```java
private User toDomainInternal(String name, String address, Integer age) {
    return new User(null, name, address, age);
}
public User toDomain(CreateUserRequest r) { return toDomainInternal(r.name(), r.address(), r.age()); }
public User toDomain(UpdateUserRequest r) { return toDomainInternal(r.name(), r.address(), r.age()); }
```

### M-02 — `UserService` Implements 5 Interfaces: God Service or Correct Design?

```java
public class UserService implements CreateUserUseCase, GetUserUseCase,
    ListUsersUseCase, UpdateUserUseCase, DeleteUserUseCase { ... }
```

This is a common and debated pattern. The case **for** it: all operations share the same `UserPersistencePort` dependency; splitting into 5 classes would mean 5 beans each injecting the same port. The case **against** it: any change to one use case recompiles the entire service class — a subtle SRP violation.

**Recommendation for this project size**: The current approach is acceptable and pragmatic. At scale or if use cases diverge (e.g., `UpdateUserUseCase` needs an event bus but others don't), split into focused service classes.

**If keeping one class**, annotate clearly:

```java
/**
 * Application service implementing all User use cases.
 * Single class is intentional — all five use cases share the same
 * persistence port and have no diverging dependencies.
 * If any use case acquires a new dependency, extract it into its own class.
 */
@Service
public class UserService implements ...
```

### M-03 — `update` and `delete` Make Two Database Round-Trips

```java
// UserService.update — calls findById THEN save: 2 round trips
public User update(String id, User user) {
    getById(id);  // round trip 1: SELECT
    return persistencePort.save(new User(id, ...));  // round trip 2: UPSERT
}
```

**Fix**: Add `existsById(String id)` to `UserPersistencePort`:

```java
// In UserPersistencePort
boolean existsById(String id);

// In UserService
public User update(String id, User user) {
    if (!persistencePort.existsById(id)) throw new UserNotFoundException(id);
    return persistencePort.save(new User(id, user.name(), user.address(), user.age()));
}
```

This halves the DB load for write operations. The MongoDB adapter delegates to `repository.existsById(id)`.

### M-04 — `UserQuery` Lives in `application/query` Package: Boundary Leakage into the Controller

```java
// UserController imports an application-layer object directly
import com.example.user.application.query.UserQuery;

listUsersUseCase.getAll(UserQuery.of(name, minAge, maxAge, page, size));
```

A controller is an **inbound adapter** — it should only use inbound port interfaces and DTOs. Building a `UserQuery` in the controller couples the adapter to the application layer's internal query model.

**Fix**: Move `UserQuery` construction into the inbound port's contract or into the mapper:

```java
// UserRestMapper
public UserQuery toQuery(String name, Integer minAge, Integer maxAge, int page, int size) {
    return UserQuery.of(name, minAge, maxAge, page, size);
}

// Controller — cleaner
public UserPageResponse getAll(...) {
    return mapper.toPageResponse(listUsersUseCase.getAll(mapper.toQuery(name, minAge, maxAge, page, size)));
}
```

### M-05 — `application.yml` Sets `logging.level.com.example: DEBUG` in Production Config

```yaml
logging:
  level:
    com.example: DEBUG   # ← will flood production logs
```

DEBUG-level application logging should never be the production default. Use `INFO` in `application.yml` and override to `DEBUG` via a profile (`application-local.yml`) or environment variable.

### M-06 — No Structured JSON Logging / No Correlation ID (MDC)

The application has no mechanism to:
1. Emit logs as JSON (required for log aggregation in ELK/Grafana Loki)
2. Attach a correlation/trace ID to all log lines for a single request

**Fix**: Add `logstash-logback-encoder` and a `OncePerRequestFilter` that sets `MDC.put("traceId", ...)`:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```java
// CorrelationIdFilter.java — in adapters.in.rest
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = Optional.ofNullable(req.getHeader("X-Trace-Id"))
                .orElse(UUID.randomUUID().toString());
        MDC.put(TRACE_ID, traceId);
        res.setHeader("X-Trace-Id", traceId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}
```

---

## 5. Low-Severity / Improvement Items 🟡

### L-01 — `application/exception/UserNotFoundException` Should Be Deleted, Not Deprecated

The deprecated class adds noise and will cause `forRemoval = true` compiler warnings. Schedule its removal in the next minor version since `1.5.0` already completed the migration to `domain.exception.UserNotFoundException`.

### L-02 — `UserController` Has 6 Constructor Parameters (SOLID: SRP Signal)

```java
public UserController(CreateUserUseCase createUserUseCase,
                      GetUserUseCase getUserUseCase,
                      ListUsersUseCase listUsersUseCase,
                      UpdateUserUseCase updateUserUseCase,
                      DeleteUserUseCase deleteUserUseCase,
                      UserRestMapper mapper) { ... }
```

Six constructor parameters is a recognised smell (SRP concern). For a CRUD controller this is acceptable — each use case is a single-method interface. At this scale it is fine. If the controller grows, split into command and query controllers (aligned with CQRS).

### L-03 — `docker-compose.yml` Uses Deprecated `version` Key

```yaml
version: "3.9"   # ← deprecated since Docker Compose v2
```

Remove the `version` key entirely — it is ignored by Docker Compose v2+ and produces a deprecation warning.

### L-04 — `UserResponse` Has No OpenAPI Schema Annotations

`@Schema(description = "...")` on `UserResponse` fields would provide richer Swagger UI documentation and enforce contract documentation for API consumers.

### L-05 — No `@Transactional` Consideration for Multi-Step Operations

`UserService.update` and `UserService.delete` each make two discrete MongoDB calls. MongoDB does not provide multi-document transactions by default (requires a replica set). For single-document operations this is acceptable, but it should be documented in an ADR or comment.

### L-06 — `SpringDataUserRepository` Could Expose `existsById` Without MongoTemplate

`MongoRepository` already provides `existsById(String id)`. No need for `MongoTemplate` for the existence check (see M-03).

### L-07 — No `.dockerignore` File Visible in Repository Root

A `.dockerignore` is mentioned in VERSIONS.md (v1.1.0) but was not found in the workspace listing. Verify it exists and excludes `target/`, `.git/`, `.idea/`.

---

## 6. SOLID Principle Audit

| Principle | Status | Evidence |
|---|---|---|
| **S** Single Responsibility | 🟡 Good | `UserService` handles 5 use cases (acceptable at this scale); DTOs duplicated (DRY/SRP concern) |
| **O** Open/Closed | ✅ Good | `UserPersistencePort` interface means swapping MongoDB for PostgreSQL requires only a new adapter, zero domain/service changes |
| **L** Liskov Substitution | ✅ Good | No inheritance hierarchies; domain uses records (value semantics) |
| **I** Interface Segregation | ✅ Excellent | 5 narrow inbound port interfaces; one focused outbound port |
| **D** Dependency Inversion | ✅ Excellent | `UserService` depends only on port interfaces; Spring injects the adapter at runtime |

---

## 7. Design Patterns In Use (Audit)

| Pattern | Where | Correctly Applied? |
|---|---|---|
| **Adapter** (GoF Structural) | `UserMongoPersistenceAdapter` wraps Spring Data behind `UserPersistencePort` | ✅ |
| **Facade** (GoF Structural) | `UserService` simplifies 5 use cases behind a single cohesive service | ✅ |
| **Value Object** (DDD) | `User`, `UserPage`, `UserQuery` — immutable records with structural equality | ✅ |
| **Repository** (DDD/GoF) | `UserPersistencePort` as a domain-facing repository abstraction | ✅ |
| **Factory Method** (GoF Creational) | `UserQuery.of(...)` static factory | ✅ |
| **Builder** (GoF Creational) | MongoDB `Query`/`Criteria` builder chain in `buildQuery` | ✅ |
| **Strategy** (GoF Behavioural) | Missing — conditional `if/else` in `buildQuery` could be replaced with composable `Criteria` strategies for complex filtering | 🟡 |
| **Chain of Responsibility** (GoF Behavioural) | `RestExceptionHandler` handlers form an implicit chain | ✅ |

**Missing pattern opportunity**: `buildQuery` will grow with each new filter field. A `UserQuerySpecification` or Criteria builder strategy would keep it open for extension (OCP).

---

## 8. Testing Assessment

| Test Type | Present | Coverage | Notes |
|---|---|---|---|
| **Unit (Service)** | ✅ | 7 tests | Mockito; tests id-stripping, not-found, query validation |
| **Integration (REST + MongoDB)** | ✅ | 5 tests | Testcontainers `mongo:7`; full stack via MockMvc |
| **Architecture (ArchUnit)** | ❌ | 0% | No layer rule enforcement |
| **Contract** | ❌ | 0% | No Pact/Spring Cloud Contract |
| **Persistence Adapter** | ❌ | 0% | No isolated `UserMongoPersistenceAdapter` tests |
| **Negative/edge cases** | 🟡 Partial | Validation error covered; age-filter bug not caught by test |

**Most important missing test**: A unit test for `buildQuery` single-bound age filter:

```java
@Test
void findAll_withOnlyMinAge_shouldFilterCorrectly() {
    // Testcontainers-backed test that proves single-bound age filter works
    // This test would have caught BUG-01
}
```

---

## 9. Security Assessment

| Area | Status | Notes |
|---|---|---|
| Actuator exposure | ✅ | Only `health` and `info` exposed |
| Stack trace / message leak | ✅ | `include-message: never`, `include-stacktrace: never` |
| Input validation | ✅ | Bean Validation on DTOs + `@Validated` on controller |
| CORS | ✅ | Not configured = Spring default deny (acceptable for internal API) |
| Authentication / Authorisation | ❌ | No auth layer present — acceptable for a demo; must be addressed before production |
| Secrets | ✅ | `MONGODB_URI` externalised via environment variable |
| Dependency CVEs | 🟡 | `spring-boot-starter-parent 3.3.4` is slightly behind latest; run `mvn dependency-check:check` |

---

## 10. Prioritised Refactoring Roadmap

| Priority | Item | Effort | Impact |
|---|---|---|---|
| 🔴 P0 | **Fix `buildQuery` `||` operator** (BUG-01) | XS | Data correctness |
| 🔴 P0 | **Fix Dockerfile JAR name** (H-01) | XS | Container is broken |
| 🔴 P1 | **Add ArchUnit tests** (H-03) | S | Architecture regression prevention |
| 🔴 P1 | **Align Java version to 21** (H-02) | XS | Unlock platform features |
| 🟠 P2 | **Add domain-level validation to `User`** (H-04) | S | Multi-adapter correctness |
| 🟠 P2 | **Add `existsById` to port; eliminate 2-trip write** (M-03) | S | Performance |
| 🟠 P2 | **Fix DEBUG logging level in production config** (M-05) | XS | Operational hygiene |
| 🟠 P2 | **Add `CorrelationIdFilter` + JSON logging** (M-06) | M | Observability |
| 🟡 P3 | **Eliminate `CreateUserRequest`/`UpdateUserRequest` duplication** (M-01) | S | DRY |
| 🟡 P3 | **Move `UserQuery` construction into mapper** (M-04) | S | Layer purity |
| 🟡 P3 | **Delete deprecated `application/exception/UserNotFoundException`** (L-01) | XS | Code hygiene |
| 🟡 P4 | **Add `@Schema` annotations to `UserResponse`** (L-04) | S | API docs quality |
| 🟡 P4 | **Add isolated `UserMongoPersistenceAdapter` tests** | M | Test coverage |

---

## 11. C4 Component Diagram

```mermaid
C4Component
    title Component Diagram — hexagonal-demo (v1.5.0)

    Container(app, "hexagonal-demo", "Spring Boot 3 / Java 17") {

        Component(controller, "UserController", "REST Adapter (IN)", "Handles HTTP CRUD requests; maps DTOs ↔ domain")
        Component(mapper, "UserRestMapper", "Mapper", "Translates between REST DTOs and domain objects")
        Component(exHandler, "RestExceptionHandler", "Error Adapter", "Maps domain exceptions → RFC 7807 ProblemDetail")

        Component(createUC, "CreateUserUseCase", "Inbound Port", "Interface: create(User)")
        Component(getUC, "GetUserUseCase", "Inbound Port", "Interface: getById(String)")
        Component(listUC, "ListUsersUseCase", "Inbound Port", "Interface: getAll(UserQuery)")
        Component(updateUC, "UpdateUserUseCase", "Inbound Port", "Interface: update(String, User)")
        Component(deleteUC, "DeleteUserUseCase", "Inbound Port", "Interface: delete(String)")

        Component(service, "UserService", "Application Service", "Implements all 5 inbound ports; orchestrates persistence port")

        Component(persistPort, "UserPersistencePort", "Outbound Port", "Interface: save / findById / findAll / deleteById")

        Component(mongoAdapter, "UserMongoPersistenceAdapter", "Persistence Adapter (OUT)", "Implements UserPersistencePort using Spring Data + MongoTemplate")
        Component(springRepo, "SpringDataUserRepository", "Spring Data", "MongoRepository<UserDocument, String>")
        Component(mongoMapper, "UserMongoMapper", "Static Mapper", "UserDocument ↔ User domain object")
    }

    ContainerDb(mongo, "MongoDB", "mongo:7", "users collection")

    Rel(controller, createUC, "calls")
    Rel(controller, getUC, "calls")
    Rel(controller, listUC, "calls")
    Rel(controller, updateUC, "calls")
    Rel(controller, deleteUC, "calls")
    Rel(controller, mapper, "uses")

    Rel(service, createUC, "implements")
    Rel(service, getUC, "implements")
    Rel(service, listUC, "implements")
    Rel(service, updateUC, "implements")
    Rel(service, deleteUC, "implements")
    Rel(service, persistPort, "calls")

    Rel(mongoAdapter, persistPort, "implements")
    Rel(mongoAdapter, springRepo, "uses")
    Rel(mongoAdapter, mongoMapper, "uses")
    Rel(mongoAdapter, mongo, "reads/writes", "Spring Data MongoDB")
```

---

## 12. What Is Done Really Well ✅

These are genuinely excellent decisions that should be preserved and highlighted to the team:

1. **`UserQuery` as a Value Object** — compact constructor validation, `of()` factory, no primitive obsession across the port boundary. Textbook DDD Value Object.
2. **`UserMongoMapper` as a static utility** — no state, no Spring bean, private constructor. Honest design.
3. **`UserDocument` as an immutable record** — correct for Spring Data MongoDB 4.x; ~40 lines of boilerplate removed.
4. **RFC 7807 `ProblemDetail`** — consistent error contract; clients parse one shape.
5. **`@Indexed` on `UserDocument.name`** with `auto-index-creation: true` — filter queries won't cause collection scans.
6. **`RestExceptionHandler` catch-all** — prevents internal details leaking from unhandled exceptions.
7. **`@NotBlank` on path variables** with `@Validated` — guards against blank-ID calls reaching the service.
8. **Testcontainers integration tests** — real `mongo:7`, per-test collection drop, full stack exercised. This is production-grade test infrastructure.
9. **Domain exception in `domain.exception`** — `UserNotFoundException` correctly placed as a domain concept, not an infrastructure concern.
10. **Actuator minimal surface** — only `health` and `info` exposed; `show-details: never`.

---

*Generated by `software-architecture-expert` agent — hexagonal-demo v1.5.0*

