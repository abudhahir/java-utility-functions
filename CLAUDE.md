# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java utility functions repository focused on providing reusable components. Currently contains:

- **request-utils**: A Spring Boot 4.0.3 module providing HTTP request utilities, built on Spring RestClient and WebMVC

The repository is structured as a multi-module project with each utility module as a subdirectory (e.g., `request-utils/`).

## Technology Stack

- Java 17
- Spring Boot 4.0.3
- Maven (with wrapper included)
- JUnit 5 for testing

## Build and Test Commands

### Building the project

```bash
# Build all modules
cd request-utils && ./mvnw clean install

# Build without running tests
cd request-utils && ./mvnw clean install -DskipTests

# Compile only
cd request-utils && ./mvnw compile
```

### Running tests

```bash
# Run all tests
cd request-utils && ./mvnw test

# Run a specific test class
cd request-utils && ./mvnw test -Dtest=RequestUtilsApplicationTests

# Run a specific test method
cd request-utils && ./mvnw test -Dtest=RequestUtilsApplicationTests#contextLoads
```

### Running the application

```bash
# Run the Spring Boot application
cd request-utils && ./mvnw spring-boot:run
```

## Project Structure

```
java-utility-functions/
├── request-utils/              # HTTP request utilities module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/cleveloper/jufu/requestutils/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── static/
│   │   │       └── templates/
│   │   └── test/
│   │       └── java/
│   │           └── com/cleveloper/jufu/requestutils/
│   ├── pom.xml
│   └── mvnw                    # Maven wrapper
└── [future utility modules will be added here]
```

## Architecture Notes

- Each utility module is a standalone Spring Boot application that can be used as a library
- Base package naming convention: `com.cleveloper.jufu.[module-name]`
- Modules use Spring Boot configuration processor for type-safe configuration properties
- The `request-utils` module provides abstractions over Spring's RestClient and WebMVC for common HTTP operations

## Development Workflow

When adding new utility modules:

1. Create a new subdirectory at the root level (sibling to `request-utils`)
2. Follow the same package structure: `com.cleveloper.jufu.[module-name]`
3. Include Maven wrapper for standalone builds
4. Each module should have its own `pom.xml` with Spring Boot parent
5. Use Java 17 language features
6. Include both unit and integration tests using JUnit 5
