# Authentication Service

This service handles authentication for a digital banking application serving millions of customers. It provides JWT-based authentication, multi-factor authentication (MFA), and session management.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/login` | Authenticate with username/password, returns JWT token |
| POST | `/auth/validate` | Validate a JWT token, check expiry, return user claims |
| POST | `/auth/mfa/verify` | Verify a one-time MFA code with a session token |
| POST | `/auth/mfa/fallback` | Initiate alternate verification when MFA device is unavailable |
| POST | `/auth/session/invalidate` | Invalidate an active session token |

## Architecture

- **TokenService** — Handles JWT token creation, validation, and expiry logic
- **SessionStore** — In-memory store tracking active sessions
- **AuthController** — REST controller exposing all authentication endpoints

## Compliance-Critical Paths

The following paths are compliance-critical and require thorough testing and monitoring:

- **Token expiry handling** — Ensures expired tokens are properly rejected and cannot be reused
- **MFA fallback flow** — Handles the case where a customer's MFA device is unavailable, triggering an alternate verification method (email or SMS)
- **Session invalidation** — Ensures sessions can be properly terminated and cannot be reused after invalidation

## Running

```bash
mvn spring-boot:run
```

## Testing

```bash
mvn test
```

## Tech Stack

- Java 17
- Spring Boot 3.2.4
- JJWT 0.12.5 (JSON Web Token library)
- JUnit 5 + Mockito (test framework)
