# auth-service — StockPro Authentication Microservice

Handles all authentication, session management, and user administration for the StockPro platform.

---

## Endpoints

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/v1/auth/register` | Public | — | Self-register (role = WAREHOUSE_STAFF) |
| POST | `/api/v1/auth/login` | Public | — | Email + password login |
| POST | `/api/v1/auth/google` | Public | — | Google OAuth2 login |
| POST | `/api/v1/auth/refresh` | Public | — | Exchange refresh token |
| POST | `/api/v1/auth/logout` | Bearer | Any | Invalidate session |
| GET | `/api/v1/auth/profile` | Bearer | Any | Get own profile |
| PUT | `/api/v1/auth/profile` | Bearer | Any | Update own profile |
| PUT | `/api/v1/auth/change-password` | Bearer | Any | Change own password |
| GET | `/api/v1/auth/validate` | Bearer | Any | Token validation (API Gateway) |
| GET | `/api/v1/auth/users` | Bearer | ADMIN | List all users |
| GET | `/api/v1/auth/users/{id}` | Bearer | ADMIN | Get user by ID |
| GET | `/api/v1/auth/users/role/{role}` | Bearer | ADMIN | Filter users by role |
| POST | `/api/v1/auth/users` | Bearer | ADMIN | Create user with any role |
| PUT | `/api/v1/auth/users/{id}` | Bearer | ADMIN | Update user |
| DELETE | `/api/v1/auth/users/{id}` | Bearer | ADMIN | Deactivate user |

---

## Tech Stack

- Java 17 + Spring Boot 3.2
- Spring Security (stateless JWT)
- Spring Data JPA + MySQL 8
- Redis (refresh token store + blacklist + session)
- Flyway (DB migrations)
- MapStruct (DTO mapping)
- JJWT 0.12 (JWT library)
- Google API Client (OAuth2 token verification)
- SpringDoc / Swagger UI
- JUnit 5 + Mockito + Testcontainers

---

## Running Locally

### Prerequisites
- Docker + Docker Compose
- Java 17 (for local development only)

### Start with Docker Compose
```bash
cd ../
docker compose -f docker-compose.all.yml up --build auth-service
```

Service will be available at `http://localhost:8083`  
Swagger UI: `http://localhost:8083/api/v1/swagger-ui.html`

### Run tests
```bash
../mvnw.cmd -pl auth-service test
```

---

## Default Admin Account

| Field | Value |
|-------|-------|
| Email | `admin@stockpro.com` |
| Password | `Admin@1234` |

> **Change this password immediately after first login in any non-local environment.**

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `stockpro_auth` | Database name |
| `DB_USER` | `root` | DB username |
| `DB_PASSWORD` | `secret` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis password |
| `JWT_SECRET` | _(dev value)_ | **Must be 256+ bits in production** |
| `JWT_ACCESS_EXPIRY` | `28800000` | Access token TTL in ms (8 hours) |
| `JWT_REFRESH_EXPIRY` | `604800000` | Refresh token TTL in ms (7 days) |
| `GOOGLE_CLIENT_ID` | _(required)_ | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | _(required)_ | Google OAuth2 client secret |
| `SERVER_PORT` | `8081` | Service port |

---

## Architecture

```
AuthController
    └── AuthService (interface)
            └── AuthServiceImpl
                    ├── UserRepository        → MySQL via JPA
                    ├── AuditLogRepository    → MySQL via JPA
                    ├── JwtUtil               → token generation/validation
                    ├── RedisTokenStore       → refresh tokens + blacklist
                    └── UserMapper            → MapStruct DTO mapping

JwtAuthFilter   → validates token on every request before hitting controllers
SecurityConfig  → stateless, public paths, RBAC via @PreAuthorize
```

---

## Security Notes

- Passwords are hashed with bcrypt (cost factor 12)
- JWT access tokens expire in **8 hours**
- Refresh tokens are stored in Redis; revoked on logout/password change/deactivation
- Logged-out access tokens are blacklisted in Redis for their remaining TTL
- All admin actions are written to `auth_audit_log`
- Optimistic locking on `User` entity prevents concurrent update races
