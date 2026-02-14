# HisabKitab Backend

A secure and scalable REST API backend for HisabKitab (Account Book) application built with Spring Boot 4.0.2 and Java 17. The API provides robust JWT-based authentication with access and refresh token support, user management, and PostgreSQL database integration.

## 🚀 Features

- **JWT Authentication**: Secure authentication with access tokens (1 hour) and refresh tokens (90 days)
- **User Management**: Registration, login, logout, and token refresh
- **Database Migrations**: Automated schema management with Flyway
- **API Documentation**: Interactive API docs with Swagger/OpenAPI
- **Security**: Spring Security with stateless session management
- **Modern Java**: Built with Java 17 and Spring Boot 4.0.2
- **Database**: PostgreSQL with JPA/Hibernate
- **Development Tools**: Lombok for reduced boilerplate, DevTools for live reload

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17** or higher ([Download](https://adoptium.net/))
- **Maven 3.6+** (or use the included Maven Wrapper)
- **PostgreSQL** (or use Supabase CLI for local development)
- **Supabase CLI** (optional, for local database) - [Installation Guide](https://supabase.com/docs/guides/cli)

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 4.0.2 | Application framework |
| Spring Security | 6.x | Authentication & authorization |
| Spring Data JPA | 3.x | Database ORM |
| PostgreSQL | - | Relational database |
| Flyway | - | Database migrations |
| Lombok | 1.18.42 | Reduce boilerplate code |
| JWT (jjwt) | 0.12.6 | Token generation/validation |
| Swagger/OpenAPI | 3.0.1 | API documentation |

## 📦 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/AsimAftab/HisabKitab-Backend.git
cd HisabKitab-Backend
```

### 2. Database Setup

#### Option A: Using Supabase CLI (Recommended for Development)

```bash
# Install Supabase CLI (if not already installed)
# See: https://supabase.com/docs/guides/cli

# Start local Supabase instance
supabase start

# The database will be available at:
# Host: localhost
# Port: 54322
# Database: postgres
# Username: postgres
# Password: postgres
```

#### Option B: Using Local PostgreSQL

1. Install PostgreSQL
2. Create a database named `hisabkitab`
3. Update `src/main/resources/application-dev.properties` with your credentials

### 3. Configure Application

The application uses profile-based configuration:

- **Development**: `application-dev.properties` (default)
- **Production**: `application-prod.properties`

**JWT Secret Configuration:**

⚠️ **Important**: Change the JWT secret in production!

Create `src/main/resources/application-local.properties` (gitignored):

```properties
jwt.secret=your-super-secret-key-at-least-256-bits-long
```

Or set as environment variable:

```bash
export JWT_SECRET=your-super-secret-key-at-least-256-bits-long
```

### 4. Build the Project

#### Using Maven Wrapper (Recommended)

**Windows:**
```bash
mvnw.cmd clean install
```

**Linux/macOS:**
```bash
./mvnw clean install
```

#### Using System Maven

```bash
mvn clean install
```

## 🚀 Running the Application

### Development Mode

**Windows:**
```bash
mvnw.cmd spring-boot:run
```

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

The application will start on **http://localhost:8080**

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=HisabKitabBackendApplicationTests
```

### Building for Production

```bash
# Create executable JAR
./mvnw clean package

# Run the JAR
java -jar target/HisabKitab-Backend-0.0.1-SNAPSHOT.jar
```

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Quick API Reference

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/v1/auth/register` | POST | Register new user | No |
| `/api/v1/auth/login` | POST | Login user | No |
| `/api/v1/auth/refresh` | POST | Refresh access token | No |
| `/api/v1/auth/logout` | POST | Logout user | No |
| `/actuator/health` | GET | Health check | Admin |

### Example: User Registration

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123",
    "fullName": "John Doe",
    "phone": "+1234567890"
  }'
```

### Example: User Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  },
  "timestamp": "2026-02-14T16:00:00"
}
```

For more detailed API documentation, see [docs/JWT_AUTHENTICATION_GUIDE.md](docs/JWT_AUTHENTICATION_GUIDE.md)

## 📁 Project Structure

```
HisabKitab-Backend/
├── src/
│   ├── main/
│   │   ├── java/com/example/hisabkitabbackend/
│   │   │   ├── auth/              # Authentication module
│   │   │   │   ├── dto/           # Data Transfer Objects
│   │   │   │   ├── service/       # Business logic
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   ├── common/            # Shared components
│   │   │   │   ├── exception/     # Exception handling
│   │   │   │   └── response/      # Response wrappers
│   │   │   ├── config/            # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── JpaConfig.java
│   │   │   ├── user/              # User domain
│   │   │   │   ├── User.java
│   │   │   │   ├── RefreshToken.java
│   │   │   │   └── Repositories...
│   │   │   └── HisabKitabBackendApplication.java
│   │   └── resources/
│   │       ├── db/migration/      # Flyway migrations
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/                      # Test files
├── docs/                          # Documentation
│   └── JWT_AUTHENTICATION_GUIDE.md
├── supabase/                      # Supabase configuration
├── pom.xml                        # Maven dependencies
├── CLAUDE.md                      # AI assistant guidelines
└── README.md                      # This file
```

## 🔐 Security

- **JWT Tokens**: Access tokens expire in 1 hour, refresh tokens in 90 days
- **Password Hashing**: BCrypt with salt for secure password storage
- **Stateless Sessions**: No server-side session management
- **CSRF Protection**: Disabled (JWT-based auth doesn't require it)
- **Refresh Token Revocation**: Logout invalidates refresh tokens in database

## 🧪 Development Guidelines

### Code Style

- Use Lombok annotations to reduce boilerplate
- Constructor injection preferred over field injection
- Follow Spring Boot best practices
- Keep controllers thin, business logic in services

### Database Migrations

- Create new migration files in `src/main/resources/db/migration/`
- Follow naming convention: `V{version}__{description}.sql`
- Example: `V3__add_user_roles.sql`

### Testing

- Write unit tests for services
- Write integration tests for controllers
- Maintain high test coverage

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 Additional Documentation

- [JWT Authentication Guide](docs/JWT_AUTHENTICATION_GUIDE.md) - Comprehensive guide to JWT implementation
- [CLAUDE.md](CLAUDE.md) - Guidelines for AI-assisted development

## 🐛 Troubleshooting

### Common Issues

**Issue**: Application fails to start with database connection error
- **Solution**: Ensure PostgreSQL is running and credentials are correct

**Issue**: JWT token validation fails
- **Solution**: Check that `jwt.secret` is properly configured and not empty

**Issue**: Lombok not working in IDE
- **Solution**: Install Lombok plugin and enable annotation processing

**Issue**: Tests failing
- **Solution**: Ensure test database is accessible and migrations are up to date

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **Asim Aftab** - [AsimAftab](https://github.com/AsimAftab)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- JWT community for authentication standards
- Supabase for database tooling

---

**Built with ❤️ using Spring Boot**
