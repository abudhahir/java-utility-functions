# JUFU Demo Web App

A Spring Boot MVC demo application showcasing user management endpoints with an embedded WireMock backend for testing and demonstration purposes.

## Overview

This application provides a production-ready REST API for user management following Apigee-style best practices:

- **Versioned API**: `/api/v1/users`
- **Consistent Error Handling**: Normalized error responses with correlation IDs
- **API Governance**: X-Correlation-Id header propagation for distributed tracing
- **OpenAPI Documentation**: Integrated SpringDoc for interactive API docs
- **Mock Backend**: JSON-file-backed WireMock stubs for deterministic testing

## Features

### Endpoints

| Method | Endpoint | Description | Status Codes |
|--------|----------|-------------|--------------|
| GET | `/api/v1/users?page=0&size=10` | List users (paginated) | 200, 500 |
| GET | `/api/v1/users/{id}` | Get user by ID | 200, 404, 500 |
| POST | `/api/v1/users` | Create new user | 201, 409, 422, 500 |
| PUT | `/api/v1/users/{id}` | Update existing user | 200, 404, 409, 422, 500 |
| DELETE | `/api/v1/users/{id}` | Delete user | 204, 404, 500 |

### API Governance

#### Correlation ID
Every request/response includes an `X-Correlation-Id` header for tracing:
- If provided by client, it's propagated through the request chain
- If not provided, the server generates a UUID

#### Error Response Format
All errors return a consistent structure:
```json
{
  "code": "USER_NOT_FOUND",
  "message": "User not found",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Error Codes
- `USER_NOT_FOUND` (404): Resource not found
- `EMAIL_CONFLICT` (409): Email already registered
- `INVALID_ROLE` (422): Invalid role value
- `INVALID_STATUS_TRANSITION` (422): Invalid status value
- `INVALID_INPUT` (422): Malformed input data
- `INTERNAL_ERROR` (500): Unexpected server error

### Seeded Users

The mock backend includes 3 pre-seeded users:

| ID | Email | Name | Role | Status |
|----|-------|------|------|--------|
| u-101 | alice@example.com | Alice Smith | ADMIN | ACTIVE |
| u-102 | bob@example.com | Bob Johnson | USER | ACTIVE |
| u-103 | charlie@example.com | Charlie Brown | USER | INACTIVE |

## Technology Stack

- **Java 17**
- **Spring Boot 4.0.3**
  - Spring Web MVC
  - Spring HATEOAS
- **SpringDoc OpenAPI 3.0.2** - API documentation
- **WireMock 2.35.0** - Mock backend (test scope)
- **Lombok** - Boilerplate reduction
- **Maven** - Build tool

## Configuration

### Application Properties

```properties
spring.application.name=jufu-demo-web-app

# Demo Backend Configuration
demo.backend.base-url=http://localhost:9999
demo.backend.connection-timeout=5000
demo.backend.read-timeout=10000
```

### WireMock Stubs

WireMock mappings are located in `src/main/resources/wiremock/mappings/` and include:

- **Success scenarios**: List, get, create, update, delete operations
- **Error scenarios**: 404 (not found), 409 (conflict), 422 (validation errors)
- **Domain rules**: Duplicate email detection, invalid role/status validation

## Building and Running

### Build the Project

```bash
./mvnw clean install
```

### Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

### Run Tests

```bash
./mvnw test
```

### Run Specific Test

```bash
./mvnw test -Dtest=UserControllerTest
```

## API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

## Example Usage

### List Users

```bash
curl -X GET "http://localhost:8080/api/v1/users?page=0&size=10" \
  -H "accept: application/json"
```

### Get User by ID

```bash
curl -X GET "http://localhost:8080/api/v1/users/u-101" \
  -H "accept: application/json" \
  -H "X-Correlation-Id: my-trace-123"
```

### Create User

```bash
curl -X POST "http://localhost:8080/api/v1/users" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "name": "New User",
    "role": "USER"
  }'
```

### Update User

```bash
curl -X PUT "http://localhost:8080/api/v1/users/u-101" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice.updated@example.com",
    "name": "Alice Smith Updated",
    "role": "ADMIN",
    "status": "ACTIVE"
  }'
```

### Delete User

```bash
curl -X DELETE "http://localhost:8080/api/v1/users/u-101" \
  -H "accept: application/json"
```

### Error Scenario - Duplicate Email

```bash
curl -X POST "http://localhost:8080/api/v1/users" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "name": "Test User",
    "role": "USER"
  }'
```

Response (409 Conflict):
```json
{
  "code": "EMAIL_CONFLICT",
  "message": "Email already registered",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Error Scenario - Invalid Role

```bash
curl -X POST "http://localhost:8080/api/v1/users" \
  -H "accept: application/json" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "Test User",
    "role": "INVALID_ROLE"
  }'
```

Response (422 Unprocessable Entity):
```json
{
  "code": "INVALID_ROLE",
  "message": "Role must be one of: ADMIN, USER",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001"
}
```

## Project Structure

```
jufu-demo-web-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cleveloper/jufu/jufudemowebapp/
│   │   │       ├── JufuDemoWebAppApplication.java
│   │   │       ├── api/
│   │   │       │   ├── CorrelationIdFilter.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       ├── config/
│   │   │       │   ├── RestClientConfig.java
│   │   │       │   └── WireMockConfig.java
│   │   │       └── user/
│   │   │           ├── ApiError.java
│   │   │           ├── CreateUserRequest.java
│   │   │           ├── PagedUserResponse.java
│   │   │           ├── UpdateUserRequest.java
│   │   │           ├── UserBackendClient.java
│   │   │           ├── UserController.java
│   │   │           └── UserDto.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── wiremock/
│   │           └── mappings/
│   │               ├── users-list.json
│   │               ├── users-get-*.json
│   │               ├── users-create-*.json
│   │               ├── users-update-*.json
│   │               └── users-delete-*.json
│   └── test/
│       └── java/
│           └── com/cleveloper/jufu/jufudemowebapp/
│               ├── JufuDemoWebAppApplicationTests.java
│               └── user/
│                   └── UserControllerTest.java
├── pom.xml
└── README.md
```

## Architecture

### Component Responsibilities

#### UserController
- REST endpoint definitions
- Request/response mapping
- HTTP status code handling
- OpenAPI annotations

#### UserBackendClient
- Backend integration via RestClient
- Error handling and logging
- Request/response transformation

#### CorrelationIdFilter
- X-Correlation-Id header management
- ThreadLocal correlation ID storage
- Request/response interception

#### GlobalExceptionHandler
- Centralized exception handling
- Error response normalization
- Status code to error code mapping

#### WireMockConfig
- WireMock lifecycle management (test scope)
- Classpath-based stub loading

## WireMock Integration

### Test Scope Only

WireMock is configured as a test dependency and runs only during integration tests. The `WireMockServer` is started in `@BeforeAll` and stopped in `@AfterAll` within test classes.

### Stub File Structure

Each WireMock stub includes:
- **request**: URL pattern, HTTP method, body patterns
- **response**: Status code, headers, JSON body
- **priority**: Lower numbers = higher priority (for overlapping patterns)

Example stub (`users-get-u101.json`):
```json
{
  "request": {
    "method": "GET",
    "urlPath": "/users/u-101"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "jsonBody": {
      "id": "u-101",
      "email": "alice@example.com",
      "name": "Alice Smith",
      "role": "ADMIN",
      "status": "ACTIVE"
    }
  }
}
```

## Testing Strategy

### Unit Tests

The `UserControllerTest` class provides comprehensive test coverage:

- ✅ List users (pagination)
- ✅ Get user by ID (success)
- ✅ Get user by ID (404 not found)
- ✅ Create user (409 duplicate email)
- ✅ Create user (422 invalid role)
- ✅ Delete user (success)
- ✅ Correlation ID propagation

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserControllerTest

# Run specific test method
./mvnw test -Dtest=UserControllerTest#testListUsers

# Run with debug output
./mvnw test -X
```

## Future Enhancements

### Potential Improvements

1. **Scenario State Management**: Implement WireMock scenarios for stateful CRUD operations
2. **Cursor Pagination**: Add cursor-based pagination alongside offset-based
3. **Rate Limiting**: Add rate limiting headers and enforcement
4. **ETag Support**: Implement conditional requests with ETag headers
5. **HATEOAS Links**: Add hypermedia links to response DTOs
6. **Metrics**: Integrate Micrometer for application metrics
7. **Security**: Add Spring Security with OAuth2/JWT
8. **Database**: Replace WireMock with actual database integration

## Use Cases

This demo app serves as:

1. **Test Bed**: For testing the `request-utils` module with realistic endpoints
2. **Integration Demo**: Showcasing best practices for REST API design
3. **Learning Resource**: Demonstrating Spring Boot MVC patterns
4. **Mock Server**: Providing deterministic backend responses for frontend development

## Contributing

When adding new endpoints:

1. Define DTOs in `user/` package
2. Add controller methods with OpenAPI annotations
3. Create corresponding WireMock stubs
4. Write integration tests
5. Update this README

## License

Part of the java-utility-functions project.

## Support

For questions or issues, please refer to the parent project documentation at `../CLAUDE.md`.

