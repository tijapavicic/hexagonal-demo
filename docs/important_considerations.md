# Important Considerations

This document captures key architectural and security decisions made in this project, with the reasoning behind each one.

---

## 1. Why CSRF is Disabled

**CSRF (Cross-Site Request Forgery) protection is irrelevant for stateless JWT APIs.** Here is the full reasoning.

### What CSRF protection actually does

CSRF attacks work by tricking a **browser** into making a request to your server using the victim's **session cookie** — the browser automatically attaches cookies to every request to the matching domain, so the malicious site's form submission "borrows" the authenticated session.

Spring's CSRF protection defends against this by requiring every state-changing request to include a **CSRF token** — a secret value stored in the session (or a cookie read by JS) that a cross-origin form cannot access.

### Why it does not apply here

This API uses **stateless JWT Bearer token authentication**:

```java
.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

| Property | Session-cookie auth | Stateless JWT auth (this project) |
|---|---|---|
| Auth credential | Session cookie — **sent automatically by browser** | `Authorization: Bearer <token>` header — **must be set explicitly by JS** |
| CSRF risk | 🔴 Real — browser sends cookie without JS involvement | ✅ None — a cross-origin attacker cannot set custom headers |
| CSRF token needed | Yes | No |

A cross-origin attacker cannot set the `Authorization: Bearer …` header from a browser form or `<img>` tag.  
The **Same-Origin Policy** blocks cross-origin JS from adding custom request headers.  
Therefore no CSRF token is needed.

### The authoritative reference

> *"If your application does not use cookies for session management (e.g. it uses stateless token-based authentication like JWT), you may safely disable CSRF protection."*  
> — [Spring Security Reference, CSRF section](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html)

### When you WOULD need CSRF back

If cookie-based sessions are ever added (e.g. for a browser-rendered UI or an OAuth2 login flow), re-enable CSRF:

```java
// Remove this:
.csrf(AbstractHttpConfigurer::disable)

// Replace with a proper token repository:
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
```

For this REST API, consumed by clients that send `Authorization` headers, disabling CSRF is **correct and intentional**.

---

## 2. How to Authenticate in Postman (avoid 403)

All `/api/users/**` endpoints require a valid JWT Bearer token. Follow these two steps.

### Step 1 — Obtain a JWT token

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "expiresInSeconds": 86400
}
```

> Default credentials are `admin` / `admin123`.  
> Override in production via `APP_USERNAME` and `APP_PASSWORD_HASH` environment variables.

### Step 2 — Attach the token to subsequent requests

In the Postman request → **Authorization** tab:

| Field | Value |
|---|---|
| **Type** | `Bearer Token` |
| **Token** | paste the `token` value from Step 1 |

Or add it manually as a header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Better: automate with a Collection variable

In the **Login** request → **Tests** tab, add this Postman script:

```javascript
const json = pm.response.json();
pm.collectionVariables.set("jwt_token", json.token);
```

Then on every other request set the **Authorization** header to:
```
Bearer {{jwt_token}}
```

Postman will automatically use the saved token for all subsequent requests without manual copy-pasting.

### Swagger UI

Navigate to `http://localhost:8080/swagger-ui.html`, click the **Authorize 🔒** button (top right),
and enter:
```
Bearer eyJhbGciOiJIUzI1NiJ9...
```
All requests from Swagger UI will then include the token automatically.

---

## 3. Keycloak Integration

As of v1.7.0 the service supports **two authentication flows** simultaneously:

| Flow | Issuer | Token type | Use case |
|------|--------|-----------|----------|
| Custom login | `POST /api/auth/login` | HMAC-SHA256 (HS256) | Internal/dev clients |
| Keycloak | `http://localhost:8180/realms/hexagonal` | RS256 (OIDC) | Enterprise SSO, frontend apps |

Both flows produce `Authorization: Bearer <token>` tokens — the composite `JwtDecoder` in
`SecurityConfig` tries HS256 first, then falls back to Keycloak's JWKS endpoint.

### How the composite decoder works

```
Request with Bearer token
        │
        ▼
 Try HMAC-SHA256 decoder  ──► success → authenticate
        │ fails (wrong signature)
        ▼
 Try Keycloak JWKS decoder ──► success → authenticate
        │ fails
        ▼
      401 Unauthorized
```

### Starting Keycloak

```bash
docker compose up keycloak
```

Keycloak starts on **port 8180** and auto-imports the `hexagonal` realm from
`keycloak/realm-export.json`. Admin UI: `http://localhost:8180` (admin / admin).

### Getting a Keycloak token — Password Grant (testuser)

```bash
curl -s -X POST http://localhost:8180/realms/hexagonal/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=hexagonal-app" \
  -d "client_secret=hexagonal-secret" \
  -d "username=testuser" \
  -d "password=testpass" | jq .access_token
```

### Getting a Keycloak token — Client Credentials (machine-to-machine)

```bash
curl -s -X POST http://localhost:8180/realms/hexagonal/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=hexagonal-app" \
  -d "client_secret=hexagonal-secret" | jq .access_token
```

### Using the token

```
GET http://localhost:8080/api/users
Authorization: Bearer <token from either flow>
```

### Keycloak realm configuration summary

| Setting | Value |
|---------|-------|
| Realm | `hexagonal` |
| Client ID | `hexagonal-app` |
| Client Secret | `hexagonal-secret` (override in production!) |
| Test user | `testuser` / `testpass` |
| Realm role | `USER` |
| Token lifetime | 86400 s (24 h) |

> **Production checklist**: change `hexagonal-secret`, use `KEYCLOAK_ISSUER_URI` env var pointing
> to the real Keycloak host, and enable `sslRequired: all` in the realm.

---

