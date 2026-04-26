# Software Architecture Expert Agent — Usage Guide

## How to Invoke

In GitHub Copilot Chat, prefix your message with the agent name:

```
@software-architecture-expert <your question or request>
```

### Quick-start examples

```
@software-architecture-expert review this project — architecture and implementation
```
```
@software-architecture-expert find all SOLID violations in UserService
```
```
@software-architecture-expert should I add CQRS to the user query flow?
```
```
@software-architecture-expert create an ADR for adding Keycloak
```

---

## Option A vs Option B — Trade-off Comparisons

When the agent detects a genuine design trade-off it presents a structured comparison:

| | Option A | Option B |
|-|----------|----------|
| **Format** | Conservative / lower-risk approach | Modern / recommended approach |
| **Pros** | Minimal change, familiar | Cleaner boundaries, better for growth |
| **Cons** | Technical debt accumulates | More up-front effort |
| **Verdict** | Suitable if X | Preferred if Y |

> **"use option b"** — typing this after a trade-off response tells the agent to implement the recommended option immediately.

---

## What the Agent Produces

| Request type | Output |
|-------------|--------|
| Architecture review | Narrative + C4 diagram (Mermaid) + SOLID audit table + refactoring roadmap |
| SOLID violation scan | Per-class violation list with severity (🔴/🟠/🟡) and concrete remediation code |
| Pattern recommendation | GoF / enterprise pattern selection with Java/Spring Boot code example |
| ADR creation | Structured ADR: Status · Context · Decision · Consequences |
| Refactoring | Direct code edits with before/after explanation |
| ArchUnit rules | Ready-to-paste test class enforcing layer rules |
| API design | OpenAPI-aligned resource model, versioning strategy, error contracts |
| Test strategy | Unit / integration / architecture / E2E breakdown per feature |

---

## Core Capabilities

### SOLID Enforcement

The agent audits every class against all five principles and flags violations with remediation:

```
@software-architecture-expert find SOLID violations in UserService.java
```

### Hexagonal Architecture Review

Checks that:
- Domain layer has **zero** framework imports (`@Document`, `@Service`, etc.)
- Application layer knows only ports — never adapters
- Adapters depend on ports — never on each other
- ArchUnit tests enforce these rules at build time

```
@software-architecture-expert verify my hexagonal boundaries are correct
```

### DDD Modelling

```
@software-architecture-expert model the User aggregate — what invariants belong in the domain?
```

### Design Pattern Selection

```
@software-architecture-expert which GoF pattern replaces this switch statement on UserType?
```

### ADR Generation

```
@software-architecture-expert write an ADR for our decision to use Keycloak over custom JWT
```

### Refactoring Roadmap

```
@software-architecture-expert create a prioritised refactoring roadmap for this codebase
```

---

## Project-Specific Context

The agent is pre-loaded with the `hexagonal-demo` layer map:

| Layer | Package | Rules |
|-------|---------|-------|
| Domain | `com.example.user.domain` | No Spring, no MongoDB, no framework |
| Application | `com.example.user.application` | Depends only on domain + port interfaces |
| REST Adapter | `com.example.user.adapters.in.rest` | Depends on inbound ports only |
| Persistence Adapter | `com.example.user.adapters.out` | Implements outbound ports, uses `@Document` |
| Config | `com.example.config` | Spring beans, security, OpenAPI |

---

## Tips for Best Results

| Do | Don't |
|----|-------|
| Ask about a specific class or layer | Ask vague "make it better" questions |
| Paste the relevant code snippet | Assume the agent already knows every file |
| Say "use option b" to trigger immediate implementation | Re-describe the same problem repeatedly |
| Ask for an ADR before a major change | Skip documentation for significant decisions |
| Request ArchUnit rules after a refactor | Let architecture drift go unenforced |

---

## Example Workflow

```
1. @software-architecture-expert review this project
   → Agent produces full review with ranked findings

2. @software-architecture-expert implement the top 3 recommendations
   → Agent edits files directly

3. @software-architecture-expert write ArchUnit rules to lock in the new boundaries
   → Agent adds/updates ArchitectureTest.java

4. @software-architecture-expert create an ADR for the changes made
   → Agent writes docs/adr/001-xxx.md
```

---

## References

The agent reasons from these canonical sources:

- *Domain-Driven Design* — Eric Evans
- *Implementing Domain-Driven Design* — Vaughn Vernon
- *Clean Architecture* — Robert C. Martin
- *Patterns of Enterprise Application Architecture* — Martin Fowler
- *Refactoring* — Martin Fowler
- *Effective Java* — Joshua Bloch
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)

