# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java utility functions repository focused on providing reusable components. Currently contains:

- **request-utils**: A Spring Boot 4.0.3 module providing HTTP request utilities, built on Spring RestClient and WebMVC
- **jufu-demo-web-app**: A production-ready demo Spring Boot web application for showcasing REST APIs and serving as a test bed for other modules
- **rule-factory**: A Java 17 utility module providing a generic predicate-based result factory (eager and lazy variants)

The repository is structured as a multi-module project with each utility module as a subdirectory (e.g., `request-utils/`, `jufu-demo-web-app/`, `rule-factory/`).

## Technology Stack

- Java 17
- Spring Boot 4.0.3
- Maven (with wrapper included)
- JUnit 5 for testing

## Build and Test Commands

### Building the project

```bash
# Build request-utils module
cd request-utils && ./mvnw clean install

# Build jufu-demo-web-app module
cd jufu-demo-web-app && ./mvnw clean install

# Build rule-factory module
cd rule-factory && mvn clean install

# Build without running tests
cd [module] && mvn clean install -DskipTests

# Compile only
cd [module] && mvn compile
```

### Running tests

```bash
# Run all tests
cd [module] && mvn test

# Run a specific test class
cd [module] && mvn test -Dtest=TestClassName

# Run a specific test method
cd [module] && mvn test -Dtest=TestClassName#methodName
```

### Running the application

```bash
# Run request-utils application
cd request-utils && ./mvnw spring-boot:run

# Run jufu-demo-web-app application
cd jufu-demo-web-app && ./mvnw spring-boot:run

# Run rule-factory Main demo
cd rule-factory && mvn -q -DskipTests compile && java -cp target/classes com.cleveloper.jufu.Main

# Run with specific port (Spring Boot modules)
cd [module] && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
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
├── jufu-demo-web-app/          # Demo REST API application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/cleveloper/jufu/jufudemowebapp/
│   │   │   │       ├── user/               # User management API
│   │   │   │       ├── api/                # API governance (filters, handlers)
│   │   │   │       └── config/             # Configuration
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── wiremock/               # WireMock stub mappings
│   │   └── test/
│   │       └── java/
│   │           └── com/cleveloper/jufu/jufudemowebapp/
│   ├── pom.xml
│   ├── mvnw
│   └── target/jufu-demo-web-app-0.0.1-SNAPSHOT.jar
├── rule-factory/               # Generic predicate result factory module
│   ├── src/
│   │   ├── main/
│   │   │   └── java/
│   │   │       └── com/cleveloper/jufu/rulefactory/
│   │   │           └── predicate/
│   │   │               ├── PredicateCondition.java     # Extended predicate interface
│   │   │               └── PredicateResultFactory.java # Eager + lazy selection API
│   │   └── test/
│   │       └── java/
│   │           └── com/cleveloper/jufu/rulefactory/predicate/
│   ├── pom.xml
│   └── README.md
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

# Java Utility Functions - Development Notes

This file contains development notes, plans, and architectural decisions for the java-utility-functions project.

---

## Recent Additions (March 9, 2026)

### Spring Modulith Documentation

Three comprehensive documents have been created to explain Spring Modulith and how to apply it to this project:

1. **[Spring Modulith Guide](docs/spring-modulith-guide.md)** (Complete Guide)
   - What is Spring Modulith and why use it
   - Core concepts and features
   - Getting started guide
   - Implementation examples
   - Real-world use cases
   - Best practices and troubleshooting
   - ~350 lines of comprehensive documentation

2. **[Spring Modulith Integration Plan](docs/plans/2026-03-09-spring-modulith-integration-plan.md)** (Implementation Plan)
   - Detailed plan for integrating Spring Modulith into request-utils
   - Current state analysis
   - Proposed modular structure
   - Phase-by-phase implementation plan (8 days)
   - Migration checklist
   - Testing strategy
   - Expected benefits

3. **[Spring Modulith Quick Reference](docs/spring-modulith-quick-reference.md)** (Cheat Sheet)
   - Quick start guide
   - Key patterns and examples
   - Common mistakes to avoid
   - Useful commands and annotations
   - One-page reference for developers

### What is Spring Modulith?

**Spring Modulith** is a toolkit for building modular monolithic applications with Spring Boot. Instead of splitting your application into microservices, it helps you build a single deployable application with clear module boundaries based on business domains.

**Key Benefits:**
- ✅ Clear module boundaries (like microservices)
- ✅ Single deployment (like monoliths)
- ✅ Auto-verified encapsulation
- ✅ Event-driven communication between modules
- ✅ Auto-generated documentation
- ✅ Easier testing and maintenance

**Example Structure:**
```
src/main/java/com/yourapp/
├── Application.java
├── order/                    # Order Module
│   ├── OrderService.java     # Public API
│   └── internal/             # Hidden from other modules
├── payment/                  # Payment Module
│   ├── PaymentService.java
│   └── internal/
└── notification/             # Notification Module
    ├── NotificationService.java
    └── internal/
```

### How It Could Apply to request-utils

The current `request-utils` project could benefit from Spring Modulith by organizing it into focused modules:

- **core** - Core condition interfaces
- **matcher** - Condition matching implementations
- **builder** - Fluent builder API
- **aop** - Annotation-based integration
- **config** - Spring Boot auto-configuration

This would provide:
- Better encapsulation
- Independent testing of modules
- Clear dependencies
- Event-driven architecture for extensibility
- Auto-generated documentation

See the integration plan for detailed implementation steps.

---

## Project Status

### request-utils Module

**Current Version:** 1.0.0-SNAPSHOT

**Completed Features:**
- ✅ Core condition matching engine
- ✅ Header and query parameter matching
- ✅ JSON payload matching (JSONPath and exact match)
- ✅ String operations (equals, contains, starts with, ends with, regex)
- ✅ AND/OR grouping with unlimited nesting
- ✅ Evaluation modes (FAIL_FAST, COLLECT_ALL)
- ✅ Fluent builder API
- ✅ Custom condition support
- ✅ AOP annotations (@JUFUMatchConditions)
- ✅ Spring Boot auto-configuration
- ✅ Comprehensive tutorial documentation

**Potential Enhancement:**
- 🔄 Spring Modulith integration (optional - see integration plan)

### jufu-demo-web-app Module

**Current Version:** 0.0.1-SNAPSHOT

**Completed Features:**
- ✅ Production-ready user management REST API (`/api/v1/users`)
- ✅ 5 CRUD endpoints (list, get, create, update, delete)
- ✅ Apigee-style API governance (correlation IDs, stable error codes)
- ✅ Centralized error handling with semantic error codes
- ✅ OpenAPI/Swagger documentation
- ✅ WireMock mock backend (19 stub mappings)
- ✅ 3 seeded users for demos
- ✅ Integration tests (7 test methods)

**Build Status:** ✅ **BUILD SUCCESSFUL**
- JAR: `target/jufu-demo-web-app-0.0.1-SNAPSHOT.jar` (41MB)
- Ready for immediate use

**Use Cases:**
- Test bed for `request-utils` module
- Demo application showcasing REST API best practices
- Integration testing with real endpoints

### rule-factory Module

**Current Version:** 1.0-SNAPSHOT

**Completed Features:**
- ✅ `PredicateCondition<T>` — extended functional interface over `Predicate<T>`
- ✅ `PredicateResultFactory.select(...)` — eager true/false candidate selection
- ✅ `PredicateResultFactory.selectLazy(...)` — lazy true/false supplier selection
- ✅ Null predicate fail-fast with `IllegalArgumentException` and explicit message
- ✅ Lazy branch-validated supplier null handling
- ✅ Exactly-once selected supplier invocation guarantee
- ✅ Predicate exception propagation unchanged
- ✅ 13 JUnit 5 tests (all passing)

**Build Status:** ✅ **BUILD SUCCESSFUL** — 13/13 tests pass
**Java Version:** 17

---

## Documentation Index

### Core Documentation
- [request-utils README](request-utils/README.md) - Main documentation
- [Tutorial Index](request-utils/docs/tutorial/00-index.md) - Getting started guide

### rule-factory Documentation
- [README](rule-factory/README.md) - Overview and API summary
- [Tutorial Index](rule-factory/docs/tutorial/00-index.md) - Learning paths and overview
- [Quick Start](rule-factory/docs/tutorial/01-quick-start.md) - First factory call in 5 minutes
- [Core Concepts](rule-factory/docs/tutorial/02-core-concepts.md) - Architecture and key types
- [Eager Selection](rule-factory/docs/tutorial/03-eager-selection.md) - Value-based selection patterns
- [Lazy Selection](rule-factory/docs/tutorial/04-lazy-selection.md) - Supplier-based deferred selection
- [Custom Predicates](rule-factory/docs/tutorial/05-custom-predicates.md) - Domain-specific predicate interfaces
- [Complete Examples](rule-factory/docs/tutorial/06-complete-examples.md) - Real-world applications
- [Troubleshooting](rule-factory/docs/tutorial/07-troubleshooting.md) - Common issues and fixes

### jufu-demo-web-app Documentation
- [README](jufu-demo-web-app/README.md) - Complete usage guide
- [READY_TO_RUN](jufu-demo-web-app/READY_TO_RUN.md) - Quick start (30 seconds)
- [IMPLEMENTATION_CHECKLIST](jufu-demo-web-app/IMPLEMENTATION_CHECKLIST.md) - Feature checklist
- [BUILD_STATUS](jufu-demo-web-app/BUILD_STATUS.md) - Build and test status

### Spring Modulith Documentation
- [Complete Guide](docs/spring-modulith-guide.md) - Everything about Spring Modulith
- [Integration Plan](docs/plans/2026-03-09-spring-modulith-integration-plan.md) - How to apply it
- [Quick Reference](docs/spring-modulith-quick-reference.md) - Cheat sheet

### Implementation Plans
- [Request Condition Matcher Design](docs/plans/2026-03-08-request-condition-matcher-design.md)
- [Request Condition Matcher Implementation](docs/plans/2026-03-08-request-condition-matcher-implementation.md)
- [JSON and AOP Implementation](docs/plans/2026-03-09-json-and-aop-implementation.md)
- [Tutorial Documentation Implementation](docs/plans/2026-03-09-tutorial-documentation-implementation.md)
- [Spring Modulith Integration Plan](docs/plans/2026-03-09-spring-modulith-integration-plan.md)

---

## Quick Start

### Running request-utils

```bash
cd request-utils
./mvnw clean install
./mvnw spring-boot:run
```

### Running jufu-demo-web-app

```bash
cd jufu-demo-web-app
./mvnw spring-boot:run
# Access API at http://localhost:8080/api/v1/users
# View Swagger UI at http://localhost:8080/swagger-ui.html
```

### Running Tests

```bash
cd [module]
./mvnw test
```


---

## Module Development Guidelines

When adding new utility modules to this repository:

1. Create a new subdirectory at the root level (sibling to `request-utils`, `jufu-demo-web-app`)
2. Follow the package structure: `com.cleveloper.jufu.[module-name]`
3. Include Maven wrapper (`mvnw` and `mvnw.cmd`) for standalone builds
4. Each module should have its own `pom.xml` with Spring Boot parent
5. Use Java 17 language features
6. Include both unit and integration tests using JUnit 5
7. Create comprehensive documentation (README, usage guides, examples)
8. Follow Apigee-style API governance for REST endpoints (stable error codes, correlation IDs, versioning)
9. Consider Spring Modulith for complex modules (see integration plan)

### Module Types

- **Utility Modules** (e.g., `request-utils`): Provide reusable functionality
  - Can be used as libraries in other projects
  - Should be well-documented with extensive examples
  
- **Demo Modules** (e.g., `jufu-demo-web-app`): Showcase features or serve as test beds
  - Demonstrate best practices
  - Provide working examples
  - Can integrate multiple utility modules

### Integration Testing

The `jufu-demo-web-app` module can be used as a test bed for `request-utils`:
- Test condition matching against real REST endpoints
- Validate AOP annotations in a running application
- Demonstrate builder API usage
- Showcase error handling patterns

---

## Notes for Future Development

### Spring Modulith Consideration

If you decide to implement Spring Modulith:
1. Read the [Complete Guide](docs/spring-modulith-guide.md) first
2. Review the [Integration Plan](docs/plans/2026-03-09-spring-modulith-integration-plan.md)
3. Follow the 8-day implementation schedule
4. Use the [Quick Reference](docs/spring-modulith-quick-reference.md) during development

**Estimated Effort:** ~42 hours (1 week full-time or 2 weeks part-time)

---

Last Updated: March 10, 2026
