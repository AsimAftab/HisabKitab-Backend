# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

This project uses Maven with the Maven Wrapper (`mvnw`):

- **Build**: `mvnw.cmd clean compile` (Windows) or `./mvnw clean compile` (Linux/macOS)
- **Run tests**: `mvnw.cmd test` or `./mvnw test`
- **Run single test class**: `mvnw.cmd test -Dtest=HisabKitabBackendApplicationTests` or `./mvnw test -Dtest=ClassName`
- **Run application**: `mvnw.cmd spring-boot:run` or `./mvnw spring-boot:run`
- **Package**: `mvnw.cmd clean package` or `./mvnw clean package`

## Project Overview

Spring Boot 4.0.2 / Java 17 REST API with JWT authentication (access + refresh tokens), PostgreSQL via Supabase local dev, Flyway migrations, and Lombok.

## Architecture

### Package Layout (`com.example.hisabkitabbackend`)

- **`auth/`** — Authentication: controller, JWT filter, DTOs, services (`AuthService`, `JwtService`), and `AuthException`
- **`user/`** — Domain entities (`User`, `RefreshToken`) and Spring Data JPA repositories
- **`common/`** — Shared `ApiResponse<T>` wrapper and `GlobalExceptionHandler` (catches `BusinessException` and generic exceptions)
- **`config/`** — `SecurityConfig` (filter chain, BCrypt, auth provider) and `JpaConfig`

### Request/Response Contract

All endpoints return `ApiResponse<T>` with `{success, message, data, timestamp}`. Errors from `GlobalExceptionHandler` return `{code, message, timestamp}` — `BusinessException` maps to 400, all others to 500.

### Authentication Flow

- **API base path**: `/api/v1/auth` (register, login, refresh, logout)
- Register/login returns `accessToken` (1hr) + `refreshToken` (90 days)
- Protected endpoints require `Authorization: Bearer <accessToken>` header
- `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) extracts JWT, validates via `JwtService`, and sets `SecurityContextHolder` authentication
- Refresh tokens are stored in DB (`refresh_tokens` table) and revoked on refresh/logout by setting `revokedAt`

### Security Configuration

- **Public**: `/api/v1/auth/**` (permitAll)
- **Actuator**: `/actuator/**` requires `ROLE_ADMIN` via HTTP Basic Auth (credentials in `application.properties`)
- **All other endpoints**: require valid JWT
- Session management: STATELESS
- JWT signing: HMAC-SHA using `jwt.secret` from config

### Database

- **PostgreSQL via Supabase CLI**: local instance on port `54322`, database `postgres`
- Start with `supabase start` from project root (config in `supabase/config.toml`)
- **Flyway migrations** in `src/main/resources/db/migration/` run on startup (`V1__init_schema.sql`, `V2__add_refresh_tokens.sql`)
- JPA `ddl-auto=update` is also enabled (Flyway runs first)

### Key Patterns

- Lombok `@RequiredArgsConstructor` for constructor injection throughout
- Entities use `@PrePersist`/`@PreUpdate` for `createdAt`/`updatedAt` timestamps
- DTOs are Java records (`RegisterRequest`, `LoginRequest`, `RefreshTokenRequest`, `AuthResponse`)
- `AuthException` extends `BusinessException` for auth-specific errors
