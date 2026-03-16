# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

This project uses Maven with the Maven Wrapper (`mvnw`):

- **Build**: `mvnw.cmd clean compile` (Windows) or `./mvnw clean compile` (Linux/macOS)
- **Run tests**: `mvnw.cmd test` or `./mvnw test`
- **Run single test class**: `mvnw.cmd test -Dtest=HisabKitabBackendApplicationTests` or `./mvnw test -Dtest=ClassName`
- **Run application**: `mvnw.cmd spring-boot:run` or `./mvnw spring-boot:run`
- **Package**: `mvnw.cmd clean package` or `./mvnw clean package`

Notes:

- Spring Boot version is `4.0.2`
- Flyway auto-configuration requires `spring-boot-starter-flyway` on Boot 4
- Offline compile in this repo can be done with system Maven if wrapper/network is unavailable

## Project Overview

Spring Boot 4.0.2 / Java 17 REST API with:

- JWT authentication with access and refresh tokens
- user profile endpoint (`/api/v1/user/me`)
- transaction CRUD for income and expense entries
- current-month balance summary
- transaction filtering by type and date range
- transaction pagination
- PostgreSQL via Supabase/dev database
- Flyway-managed schema migrations
- request logging for diagnosis

## Architecture

### Package Layout (`com.example.hisabkitabbackend`)

- **`auth/`** — Authentication: controller, JWT filter, DTOs, services (`AuthService`, `JwtService`), and `AuthException`
- **`user/`** — User profile, `User`, `RefreshToken`, repositories, and `UserController`
- **`transaction/`** — `Transaction` entity, repository, service, controller, DTOs, monthly summary projection
- **`common/`** — Shared `ApiResponse<T>`, `PagedResponse<T>`, and `GlobalExceptionHandler`
- **`config/`** — security, JPA, OpenAPI, request logging, and API cache control filters

### Request/Response Contract

All endpoints return `ApiResponse<T>` with `{success, message, data, timestamp}`.

List endpoints can return `PagedResponse<T>` inside `data` with:

- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`
- `hasNext`
- `hasPrevious`

`GlobalExceptionHandler` returns 400 for `BusinessException`, 400 for validation errors, and 500 for unexpected exceptions.

### Authentication Flow

- **API base path**: `/api/v1/auth` (register, login, refresh, logout)
- Register/login returns `accessToken` (1hr) + `refreshToken` (90 days)
- Protected endpoints require `Authorization: Bearer <accessToken>` header
- `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) extracts JWT, validates via `JwtService`, and sets `SecurityContextHolder` authentication
- Refresh tokens are stored in DB (`refresh_tokens` table) and revoked on refresh/logout by setting `revokedAt`

### Security Configuration

- **Public**: `/api/v1/auth/**` (permitAll)
- **Actuator**: `/actuator/**` is public in current config
- **All other endpoints**: require valid JWT
- Session management: STATELESS
- JWT signing: HMAC-SHA using `jwt.secret` from config

### Database

- **Primary dev DB**: currently points at the shared Supabase/PostgreSQL database from `application-dev.properties`
- **Optional local DB**: Supabase CLI config exists in `supabase/config.toml`
- **Flyway migrations** live in `src/main/resources/db/migration/`
- Current migrations include user/auth setup plus transaction table creation
- JPA is configured with `ddl-auto=none`; schema changes must go through Flyway
- Current dev configuration uses `spring.flyway.baseline-version=2` because the shared dev database already had `users` and `refresh_tokens` before Flyway history was created

### Key Patterns

- Lombok `@RequiredArgsConstructor` for constructor injection throughout
- Entities use `@PrePersist`/`@PreUpdate` for `createdAt`/`updatedAt` timestamps
- DTOs are Java records
- `AuthException` extends `BusinessException` for auth-specific errors
- Request logging is implemented via `OncePerRequestFilter`
- API responses set no-cache headers for `/api/**`

## Current API Surface

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### User

- `GET /api/v1/user/me`

### Transactions

- `POST /api/v1/transactions`
- `GET /api/v1/transactions`
- `GET /api/v1/transactions?type=EXPENSE`
- `GET /api/v1/transactions?from=2026-03-01&to=2026-03-31&page=0&size=20`
- `GET /api/v1/transactions/summary/current-month`
- `PUT /api/v1/transactions/{transactionId}`
- `DELETE /api/v1/transactions/{transactionId}`

Transaction query parameters:

- `type` is optional: `EXPENSE` or `INCOME`
- `from` and `to` must be provided together if used
- same `from` and `to` means one-day filtering
- pagination defaults: `page=0`, `size=20`
- page must be `>= 0`
- size must be between `1` and `100`

## Operational Notes

- The request log format includes method, full path, status, duration, IP, and user
- Production logging should keep `logging.level.com.example.hisabkitabbackend=INFO` unless deeper debugging is required
- If Flyway appears not to run, check that `spring-boot-starter-flyway` is present and inspect startup logs before Hibernate initialization
- If using the shared dev DB and `flyway_schema_history` is recreated incorrectly, baseline handling matters; do not casually change `spring.flyway.baseline-version`
