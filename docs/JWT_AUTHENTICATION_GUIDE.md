# JWT Authentication Guide - Hisab Kitab Backend

## Table of Contents
1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Architecture Overview](#architecture-overview)
4. [JWT Flow Explained](#jwt-flow-explained)
5. [Component Breakdown](#component-breakdown)
6. [Configuration](#configuration)
7. [API Endpoints](#api-endpoints)
8. [Testing Guide](#testing-guide)
9. [Spring Boot Concepts Refresher](#spring-boot-concepts-refresher)

---

## Project Overview

Hisab Kitab Backend is a RESTful API service built with Spring Boot 4.0.2 that provides user authentication using JWT (JSON Web Tokens). The application supports:

- User registration with email/password
- Login with JWT access and refresh tokens
- Token refresh mechanism
- Secure endpoints protected by JWT

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 4.0.2 | Application framework |
| Spring Security | 6.x | Security & authentication |
| Spring Data JPA | 3.x | Database access |
| PostgreSQL | - | Database |
| Flyway | - | Database migrations |
| Lombok | 1.18.42 | Reduce boilerplate code |
| JWT (jjwt) | 0.12.6 | Token generation/validation |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (App/Browser)                     │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     AuthController                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ Register │ │  Login   │ │ Refresh  │ │  Logout  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                       AuthService                                │
│  - register()    - login()                                      │
│  - refreshToken() - logout()                                     │
└─────────────────────────────────────────────────────────────────┘
          │               │                      │
          ▼               ▼                      ▼
┌──────────────┐  ┌──────────────┐    ┌──────────────┐
│  UserRepository│  │ JwtService   │    │RefreshToken  │
│               │  │              │    │  Repository  │
└──────────────┘  └──────────────┘    └──────────────┘
                          │
                          ▼
              ┌──────────────────────┐
              │   PostgreSQL DB      │
              │  - users             │
              │  - refresh_tokens    │
              └──────────────────────┘
```

---

## JWT Flow Explained

### What is JWT?

JWT (JSON Web Token) is a compact, URL-safe means of representing claims to be transferred between two parties. It consists of three parts:

```
Header.Payload.Signature
```

**Example:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGVtYWlsLmNvbSIsImlhdCI6MTYxNjIzOTAyMiwiZXhwIjoxNjE2MjQyODIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

### Authentication Flow Diagram

```
┌───────┐                    ┌──────────────┐                    ┌───────┐
│       │   1. Register/Login│              │   2. Validate &    │       │
│Client │ ──────────────────►│   /api/auth  │ ──────────────────►│  DB   │
│       │                    │   Controller │                    │       │
└───────┘                    └──────────────┘                    └───────┘
   ▲                                │
   │                                │ 3. Generate Tokens
   │                                │    - Access Token (1 hour)
   │                                │    - Refresh Token (7 days)
   │                                ▼
   │                    ┌──────────────────┐
   │   6. New Tokens   │   JwtService     │
   │◄──────────────────│                  │
   │                    └──────────────────┘
   │                                │
   │  4. Return Tokens              │ 5. Store Refresh Token
   │◄───────────────────────────────┴──────────────────────►
   │                                                    ┌──────────┐
   │                                                    │    DB    │
   │                                                    │(refresh_ │
   │                                                    │ tokens)  │
   │                                                    └──────────┘
│
│  Subsequent Requests (Protected Endpoints)
│
┌───────┐                    ┌──────────────┐                    ┌───────┐
│       │   7. Request with │              │   8. Validate JWT  │       │
│Client │ ──────────────────►│  Any Protected│ ──────────────────►│Filter │
│       │   Authorization:  │   Endpoint    │                    │       │
│       │   Bearer <token>   │              │                    └───────┘
└───────┘                    └──────────────┘                          │
   ▲                                                                  │
   │                              9. Set SecurityContext             │
   │◄─────────────────────────────────────────────────────────────────┘
   │
│  Token Refresh (When Access Token Expires)
│
┌───────┐                    ┌──────────────┐                    ┌───────┐
│       │   10. Send Refresh│              │   11. Validate &  │       │
│Client │ ──────────────────►│  /api/auth/  │ ──────────────────►│  DB   │
│       │   Token           │   refresh     │                    │       │
└───────┘                    └──────────────┘                    └───────┘
   ▲                                │
   │                                │ 12. Generate New Tokens
   │  13. New Access & Refresh      │
   │      Tokens ◄───────────────────┘
```

### Why Access Token + Refresh Token?

| Token Type | Expires In | Stored In | Purpose |
|------------|------------|-----------|---------|
| **Access Token** | 1 hour | Client only | Quick access to protected resources |
| **Refresh Token** | 7 days | Database | Get new access token without re-login |

**Benefits:**
- Short-lived access tokens = better security if leaked
- Refresh tokens stored in DB = can be revoked (logout)
- User stays logged in without re-entering credentials

---

## Component Breakdown

### 1. AuthController (`src/main/java/.../auth/AuthController.java`)

**Purpose:** REST API endpoints for authentication.

**Key Annotations:**
- `@RestController` = Combines `@Controller` + `@ResponseBody` (returns JSON)
- `@RequestMapping("/api/auth")` = Base path for all endpoints
- `@RequiredArgsConstructor` = Lombok generates constructor for final fields

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor  // Lombok: generates constructor for 'final authService'
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest request) {
        // Returns standardized response wrapper
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }
}
```

### 2. AuthService (`src/main/java/.../auth/service/AuthService.java`)

**Purpose:** Business logic for authentication operations.

**Key Annotations:**
- `@Service` = Spring component (business logic layer)
- `@Transactional` = Database operations are atomic (all or nothing)

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email exists
        // 2. Validate password
        // 3. Hash password
        // 4. Create user
        // 5. Generate tokens
        // 6. Save refresh token to DB
        // 7. Return response
    }
}
```

### 3. JwtService (`src/main/java/.../auth/service/JwtService.java`)

**Purpose:** Generate, validate, and extract data from JWT tokens.

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")  // Reads from application.properties
    private String jwtSecret;

    // Generate access token (1 hour expiry)
    public String generateAccessToken(String email) { ... }

    // Generate refresh token (7 days expiry)
    public String generateRefreshToken(String email) { ... }

    // Extract email from token
    public String extractEmail(String token) { ... }

    // Validate token
    public boolean isTokenValid(String token) { ... }
}
```

### 4. UserRepository & RefreshTokenRepository (`src/main/java/.../user/`)

**Purpose:** Database access layer using Spring Data JPA.

```java
// No implementation needed! Spring creates it at runtime.
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);  // Spring generates SQL automatically
    boolean existsByEmail(String email);
}
```

**How Spring Data JPA Works:**
- You define an interface extending `JpaRepository`
- Spring implements it at runtime using proxies
- Method names are converted to SQL queries:
  - `findByEmail` → `SELECT * FROM users WHERE email = ?`

### 5. User Entity (`src/main/java/.../user/User.java`)

**Purpose:** Represents the `users` table in the database.

```java
@Entity  // Marks this class as a database table
@Table(name = "users")  // Table name (optional, defaults to class name)
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
@Builder  // Lombok: generates builder pattern
@NoArgsConstructor  // Required by JPA
@AllArgsConstructor  // For convenience
public class User {
    @Id  // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-generate ID
    private UUID id;

    @Column(unique = true, nullable = false)  // Email is unique & required
    private String email;

    @Column(nullable = false)
    private String passwordHash;  // Never store plain passwords!
}
```

### 6. JwtAuthenticationFilter (`src/main/java/.../auth/JwtAuthenticationFilter.java`)

**Purpose:** Intercept every request, extract JWT from header, validate it, set authentication.

**Executes BEFORE your controller methods!**

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {
        // 1. Extract Authorization header
        // 2. Check if it starts with "Bearer "
        // 3. Validate token
        // 4. Load user from database
        // 5. Set authentication in SecurityContext
        // 6. Continue to next filter/controller
    }
}
```

### 7. SecurityConfig (`src/main/java/.../config/SecurityConfig.java`)

**Purpose:** Configure Spring Security - which endpoints are public, which are protected.

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF (we use JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No sessions
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Public endpoints
                .requestMatchers("/actuator/**").permitAll()  // Health checks
                .anyRequest().authenticated()  // Everything else requires JWT
            )
            .addFilterBefore(jwtAuthenticationFilter,
                           UsernamePasswordAuthenticationFilter.class);  // Add JWT filter

        return http.build();
    }
}
```

### 8. UserDetailsServiceImpl (`src/main/java/.../auth/UserDetailsServiceImpl.java`)

**Purpose:** Bridge between our `User` entity and Spring Security's `UserDetails`.

```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Convert our User to Spring's UserDetails
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .disabled(!user.getIsActive())
            .build();
    }
}
```

---

## Configuration

### application.properties

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:54322/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres

# Hibernate - auto-create tables from entities
spring.jpa.hibernate.ddl-auto=update

# Flyway - run migrations before Hibernate validates
spring.jpa.defer-datasource-initialization=true
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT Configuration
jwt.secret=YOUR_SUPER_SECRET_KEY_CHANGE_IN_PRODUCTION  # Change this in production!
jwt.access-token-expiration=3600000  # 1 hour in milliseconds
jwt.refresh-token-expiration=604800000  # 7 days in milliseconds
```

### Database Migrations

Located in: `src/main/resources/db/migration/`

- **V1__init_schema.sql** - Creates `users` table
- **V2__add_refresh_tokens.sql** - Creates `refresh_tokens` table

Flyway runs these in order on application startup.

---

## API Endpoints

### Public Endpoints (No Authentication Required)

#### 1. Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123",
  "fullName": "John Doe",
  "phone": "+1234567890"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  },
  "timestamp": "2025-02-09T10:30:00"
}
```

#### 2. Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "Password123"
}
```

**Response:** Same as register

#### 3. Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGci..."
}
```

**Response:** New access and refresh tokens

#### 4. Logout
```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "eyJhbGci..."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null,
  "timestamp": "2025-02-09T10:30:00"
}
```

### Protected Endpoints (Require JWT)

Add the Authorization header:
```http
Authorization: Bearer <your-access-token>
```

---

## Testing Guide

### Using cURL

**1. Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"Password123\",\"fullName\":\"Test User\"}"
```

**2. Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"Password123\"}"
```

**3. Access Protected Endpoint:**
```bash
curl http://localhost:8080/api/some-protected-endpoint \
  -H "Authorization: Bearer <your-access-token>"
```

**4. Refresh Token:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<your-refresh-token>\"}"
```

**5. Logout:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<your-refresh-token>\"}"
```

### Using Postman

1. **Import the endpoints** into Postman
2. **Set environment variable** `baseUrl` = `http://localhost:8080`
3. **For protected endpoints:**
   - Go to Authorization tab
   - Type: Bearer Token
   - Token: Paste your access token

---

## Spring Boot Concepts Refresher

### Dependency Injection (DI)

Spring manages objects (beans) and injects them where needed.

```java
@Service
public class AuthService {
    private final UserRepository userRepository;  // Spring injects this

    // Constructor injection (preferred)
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**With Lombok (`@RequiredArgsConstructor`):**
```java
@Service
@RequiredArgsConstructor  // Generates constructor for final fields
public class AuthService {
    private final UserRepository userRepository;  // No constructor needed!
}
```

### Annotations Quick Reference

| Annotation | Purpose | Used On |
|------------|---------|---------|
| `@Entity` | Marks class as database table | Class |
| `@Table` | Specify table name | Class |
| `@Id` | Primary key | Field |
| `@Column` | Column properties | Field |
| `@RestController` | REST controller (JSON responses) | Class |
| `@RequestMapping` | Base URL for endpoints | Class/Method |
| `@GetMapping` | HTTP GET endpoint | Method |
| `@PostMapping` | HTTP POST endpoint | Method |
| `@Service` | Business logic layer | Class |
| `@Repository` | Data access layer | Interface |
| `@Component` | General Spring bean | Class |
| `@Configuration` | Configuration class | Class |
| `@Bean` | Define a bean manually | Method |
| `@Autowired` | Inject dependency (constructor preferred) | Field/Constructor |
| `@Value` | Inject value from properties | Field |
| `@Transactional` | Transactional method | Method/Class |
| `@RequestBody` | Bind HTTP body to parameter | Parameter |
| `@PathVariable` | Bind path variable to parameter | Parameter |
| `@RequestParam` | Bind query parameter to parameter | Parameter |

### Repository Methods Convention

| Method Name | Generated SQL |
|-------------|---------------|
| `findByEmail(String email)` | `SELECT * FROM table WHERE email = ?` |
| `existsByEmail(String email)` | `SELECT COUNT(*) > 0 WHERE email = ?` |
| `deleteById(UUID id)` | `DELETE FROM table WHERE id = ?` |
| `findByEmailAndIsActive(String email, boolean active)` | `SELECT * WHERE email = ? AND is_active = ?` |

### What Happens on Startup?

```
1. Spring Boot starts
   │
2. Loads application.properties
   │
3. Scans for @Component, @Service, @Repository, @Controller, @Configuration
   │
4. Creates beans and injects dependencies
   │
5. Flyway runs database migrations (V1, V2, ...)
   │
6. Hibernate validates entities against database schema
   │
7. Tomcat server starts on port 8080
   │
8. Application is ready!
```

---

## Common Issues & Solutions

### Issue 1: JWT Expired
**Solution:** Use the refresh endpoint to get a new access token.

### Issue 2: "User not found"
**Solution:** Register first before trying to login.

### Issue 3: 401 Unauthorized on protected endpoint
**Solution:** Make sure you're sending `Authorization: Bearer <token>` header with a valid access token.

### Issue 4: Lombok not working in IntelliJ
**Solution:**
1. Install Lombok plugin
2. Enable annotation processing
3. Invalidate caches and restart

---

## Next Steps

- Add role-based authorization (ADMIN, USER)
- Add email verification
- Add password reset flow
- Add rate limiting
- Add OAuth2 (Google, GitHub login)
