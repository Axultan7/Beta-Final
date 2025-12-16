# Microservices Application

A complete microservices application built with Java Spring Boot 3, featuring authentication, user management, and API gateway routing.

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2.x**
- **Spring Security** (JWT authentication)
- **Spring Data JPA** (Hibernate ORM)
- **Spring Cloud Gateway**
- **PostgreSQL 14+**
- **Redis 7.x** (caching and session management)
- **Docker & Docker Compose**
- **Swagger/Springdoc OpenAPI**
- **Maven**
- **Lombok**
- **MapStruct**

## Project Structure

```
├── api-gateway/          # API Gateway service (port 8080)
├── auth-service/         # Authentication service (port 8081)
├── user-service/         # User management service (port 8082)
├── shared-lib/           # Common classes and utilities
├── init-scripts/         # Database initialization scripts
├── docker-compose.yml    # Docker orchestration
└── pom.xml              # Parent Maven POM
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker and Docker Compose
- Git

### Running with Docker Compose

1. **Clone and navigate to the project:**
   ```bash
   cd "Beta Final"
   ```

2. **Build the project:**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Start all services:**
   ```bash
   docker-compose up -d
   ```

4. **Verify services are running:**
   ```bash
   docker-compose ps
   ```

### Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | Main entry point |
| Auth Service | http://localhost:8081 | Authentication |
| User Service | http://localhost:8082 | User management |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |

## API Documentation

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | User login |
| POST | `/api/v1/auth/logout` | User logout |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| GET | `/api/v1/auth/validate` | Validate token |
| GET | `/api/v1/auth/me` | Get current user |

### User Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/users` | Get all users (paginated) |
| GET | `/api/v1/users/{id}` | Get user by ID |
| POST | `/api/v1/users` | Create new user |
| PUT | `/api/v1/users/{id}` | Update user |
| DELETE | `/api/v1/users/{id}` | Delete user |
| GET | `/api/v1/users/search` | Search users |

## Example API Requests

### Register a New User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### Get All Users (with authentication)
```bash
curl -X GET "http://localhost:8080/api/v1/users?page=0&size=20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### Create User (with authentication)
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "email": "newuser@example.com",
    "password": "password123",
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_PASSWORD | secure_password | Database password |
| REDIS_PASSWORD | redis_password | Redis password |
| JWT_SECRET | (base64 encoded) | JWT signing key |
| JWT_EXPIRATION | 900000 | Access token TTL (15 min) |
| REFRESH_TOKEN_EXPIRATION | 604800000 | Refresh token TTL (7 days) |

### Redis Cache Keys

| Key Pattern | Description | TTL |
|-------------|-------------|-----|
| `user:{id}` | User data cache | 1 hour |
| `refresh_token:{token}` | Refresh tokens | 7 days |
| `blacklist:{token}` | Invalidated tokens | 15 min |
| `rate_limit:{ip}` | Rate limiting | 1 min |

## Development

### Running Locally (without Docker)

1. **Start PostgreSQL and Redis:**
   ```bash
   docker-compose up -d postgres-auth postgres-user redis
   ```

2. **Build shared library:**
   ```bash
   mvn clean install -pl shared-lib
   ```

3. **Run services individually:**
   ```bash
   # Auth Service
   cd auth-service && mvn spring-boot:run

   # User Service
   cd user-service && mvn spring-boot:run

   # API Gateway
   cd api-gateway && mvn spring-boot:run
   ```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl auth-service
mvn test -pl user-service
```

## Security Features

- **JWT Authentication** with access and refresh tokens
- **Password hashing** using BCrypt
- **Token blacklisting** for logout
- **Rate limiting** (60 requests/minute per IP)
- **CORS configuration**
- **Input validation**

## Monitoring

Health endpoints are available at:
- http://localhost:8080/actuator/health
- http://localhost:8081/actuator/health
- http://localhost:8082/actuator/health

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## License

This project is for educational purposes.
