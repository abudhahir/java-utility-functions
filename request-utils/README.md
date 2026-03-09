# Request Utils

A Spring Boot utility module providing powerful HTTP request condition matching for routing, filtering, and validation scenarios.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Concepts](#core-concepts)
- [Usage Guide](#usage-guide)
  - [Basic Header Matching](#basic-header-matching)
  - [Query Parameter Matching](#query-parameter-matching)
  - [Combining Conditions with AND](#combining-conditions-with-and)
  - [Combining Conditions with OR](#combining-conditions-with-or)
  - [Nested Groups](#nested-groups)
  - [Case-Insensitive Matching](#case-insensitive-matching)
  - [Pattern Matching with Regex](#pattern-matching-with-regex)
  - [Evaluation Modes](#evaluation-modes)
  - [Custom Conditions](#custom-conditions)
  - [Annotation-Based (AOP) Usage](#annotation-based-aop-usage)
- [Exception Handling](#exception-handling)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Examples](#examples)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

## Overview

Request Utils provides a flexible condition matching engine for HTTP requests. Evaluate headers, query parameters, and JSON payloads against configured conditions with detailed success/failure reporting. Use programmatic APIs or declarative annotations for clean, maintainable code.

**Key Use Cases:**
- Request routing based on headers or parameters
- API versioning and feature flags
- Request validation and filtering
- Multi-tenancy routing
- A/B testing and canary deployments

## Features

✅ **Currently Available:**
- Header condition matching
- Query parameter condition matching
- JSON payload matching (JSONPath and exact field matching)
- String operations: equals, contains, starts with, ends with, regex
- Case-sensitive and case-insensitive matching
- Boolean logic: AND/OR groups with unlimited nesting
- Two evaluation modes: FAIL_FAST (performance) and COLLECT_ALL (debugging)
- Detailed failure reporting
- Fluent builder API
- Custom condition support
- Annotation-based AOP integration (`@JUFUMatchConditions`)
- Declarative condition definitions with inline annotations
- Spring Boot auto-configuration
- Zero-configuration setup

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.cleveloper.jufu</groupId>
    <artifactId>request-utils</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```gradle
implementation 'com.cleveloper.jufu:request-utils:1.0.0-SNAPSHOT'
```

**Requirements:**
- Java 17 or higher
- Spring Boot 4.0.3 or higher

## Quick Start

### 1. Inject the Matcher Service

```java
import com.cleveloper.jufu.requestutils.condition.core.RequestConditionMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestRouter {

    private final RequestConditionMatcher matcher;

    @Autowired
    public RequestRouter(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }
}
```

### 2. Create a Simple Condition

```java
import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import jakarta.servlet.http.HttpServletRequest;

public void routePremiumUsers(HttpServletRequest request) {
    // Match requests with header "X-User-Type: premium"
    Condition condition = new HeaderCondition(
        "X-User-Type",
        "premium",
        MatchOperation.EQUALS,
        false
    );

    ConditionResult result = matcher.evaluate(condition, request);

    if (result.isMatched()) {
        // Route to premium endpoint
    } else {
        // Route to standard endpoint
    }
}
```

### 3. Handle Failures

```java
import com.cleveloper.jufu.requestutils.condition.aop.ConditionNotMetException;

public void validateRequest(HttpServletRequest request) {
    Condition condition = new HeaderCondition(
        "X-Api-Key",
        "valid-key",
        MatchOperation.EQUALS,
        false
    );

    ConditionResult result = matcher.evaluate(condition, request);

    if (!result.isMatched()) {
        throw new ConditionNotMetException(result);
        // Exception message: "Header 'X-Api-Key' expected to equal 'valid-key' but was 'invalid-key'"
    }
}
```

## Core Concepts

### Condition

A `Condition` is a functional interface representing a single evaluation rule:

```java
@FunctionalInterface
public interface Condition {
    ConditionResult evaluate(RequestContext context);
}
```

**Built-in Conditions:**
- `HeaderCondition` - Matches HTTP headers
- `QueryParamCondition` - Matches query parameters
- Custom implementations - Implement `Condition` for business logic

### ConditionGroup

A `ConditionGroup` combines multiple conditions with AND/OR logic:

```java
ConditionGroup group = ConditionGroup.and(
    new HeaderCondition("X-Type", "premium", MatchOperation.EQUALS, false),
    new QueryParamCondition("version", "v2", MatchOperation.STARTS_WITH, false)
);
```

### ConditionResult

The result of condition evaluation:

```java
public class ConditionResult {
    boolean isMatched();                    // true if all conditions passed
    List<ConditionFailure> getFailures();   // details of failures (empty if matched)
}
```

### ConditionFailure

Detailed information about a failed condition:

```java
public class ConditionFailure {
    String getConditionType();    // e.g., "Header", "QueryParam"
    String getFieldName();        // e.g., "X-Api-Key"
    String getOperation();        // e.g., "equals"
    String getExpectedValue();    // e.g., "premium"
    String getActualValue();      // e.g., "basic" or "[not present]"
    String getMessage();          // Human-readable description
}
```

### MatchOperation

String matching operations:

```java
public enum MatchOperation {
    EQUALS,        // Exact match
    CONTAINS,      // Substring match
    STARTS_WITH,   // Prefix match
    ENDS_WITH,     // Suffix match
    REGEX          // Pattern match
}
```

### EvaluationMode

Controls evaluation behavior:

```java
public enum EvaluationMode {
    FAIL_FAST,     // Stop at first failure (better performance)
    COLLECT_ALL    // Evaluate all and collect all failures (better debugging)
}
```

## Usage Guide

### Basic Header Matching

Match requests with specific headers:

```java
// Exact match
Condition exactMatch = new HeaderCondition(
    "X-Api-Version",
    "2.0",
    MatchOperation.EQUALS,
    false
);

// Contains substring
Condition containsMatch = new HeaderCondition(
    "User-Agent",
    "Mobile",
    MatchOperation.CONTAINS,
    false
);

// Starts with prefix
Condition startsWithMatch = new HeaderCondition(
    "Authorization",
    "Bearer ",
    MatchOperation.STARTS_WITH,
    false
);

// Ends with suffix
Condition endsWithMatch = new HeaderCondition(
    "Content-Type",
    "/json",
    MatchOperation.ENDS_WITH,
    false
);

ConditionResult result = matcher.evaluate(exactMatch, request);
```

### Query Parameter Matching

Match requests with specific query parameters:

```java
// Match version parameter
Condition versionCheck = new QueryParamCondition(
    "version",
    "v2",
    MatchOperation.EQUALS,
    false
);

// Check for feature flag
Condition featureFlag = new QueryParamCondition(
    "feature",
    "beta",
    MatchOperation.CONTAINS,
    false
);

// Validate format parameter
Condition formatCheck = new QueryParamCondition(
    "format",
    "json",
    MatchOperation.EQUALS,
    false
);

ConditionResult result = matcher.evaluate(versionCheck, request);
```

### Combining Conditions with AND

All conditions must pass:

```java
// Using static factory
ConditionGroup premiumUserCheck = ConditionGroup.and(
    new HeaderCondition("X-User-Type", "premium", MatchOperation.EQUALS, false),
    new HeaderCondition("X-Api-Key", "valid-key", MatchOperation.EQUALS, false),
    new QueryParamCondition("version", "v2", MatchOperation.STARTS_WITH, false)
);

ConditionResult result = matcher.evaluate(premiumUserCheck, request);

if (result.isMatched()) {
    // All three conditions passed
} else {
    // At least one condition failed
    for (ConditionFailure failure : result.getFailures()) {
        System.out.println(failure.getMessage());
    }
}
```

### Combining Conditions with OR

At least one condition must pass:

```java
// Accept requests from either mobile or tablet
ConditionGroup mobileOrTablet = ConditionGroup.or(
    new HeaderCondition("User-Agent", "Mobile", MatchOperation.CONTAINS, false),
    new HeaderCondition("User-Agent", "Tablet", MatchOperation.CONTAINS, false)
);

ConditionResult result = matcher.evaluate(mobileOrTablet, request);

if (result.isMatched()) {
    // At least one condition passed (mobile or tablet)
}
```

### Nested Groups

Create complex boolean expressions:

```java
import com.cleveloper.jufu.requestutils.condition.builder.ConditionGroupBuilder;

// (Premium user OR Admin) AND (Version 2 OR Version 3)
Condition condition = ConditionGroup.builder()
    .or(
        new HeaderCondition("X-User-Type", "premium", MatchOperation.EQUALS, false),
        new HeaderCondition("X-User-Type", "admin", MatchOperation.EQUALS, false)
    )
    .and(
        ConditionGroup.or(
            new QueryParamCondition("version", "v2", MatchOperation.EQUALS, false),
            new QueryParamCondition("version", "v3", MatchOperation.EQUALS, false)
        )
    )
    .build();

ConditionResult result = matcher.evaluate(condition, request);
```

### Builder API for Complex Conditions

Fluent API for readable condition building:

```java
Condition condition = ConditionGroup.builder()
    // First level: AND group
    .and(new HeaderCondition("X-Api-Key", "valid", MatchOperation.EQUALS, false))
    .and(new QueryParamCondition("format", "json", MatchOperation.EQUALS, false))

    // Nested OR group
    .andGroup(group -> group
        .or(new HeaderCondition("X-Region", "US", MatchOperation.EQUALS, false))
        .or(new HeaderCondition("X-Region", "EU", MatchOperation.EQUALS, false))
    )

    // Set evaluation mode
    .mode(EvaluationMode.COLLECT_ALL)
    .build();

ConditionResult result = matcher.evaluate(condition, request);
```

### Case-Insensitive Matching

Use the `ignoreCase` parameter:

```java
// Case-insensitive header matching
Condition caseInsensitive = new HeaderCondition(
    "Content-Type",
    "APPLICATION/JSON",  // Will match "application/json", "Application/JSON", etc.
    MatchOperation.EQUALS,
    true  // ignoreCase = true
);

// Case-insensitive contains
Condition browserCheck = new HeaderCondition(
    "User-Agent",
    "CHROME",  // Will match "chrome", "Chrome", "CHROME"
    MatchOperation.CONTAINS,
    true
);

ConditionResult result = matcher.evaluate(caseInsensitive, request);
```

### Pattern Matching with Regex

Use regular expressions for advanced matching:

```java
// Match semantic versioning (e.g., v1.2.3, v2.0.0)
Condition versionPattern = new QueryParamCondition(
    "version",
    "v\\d+\\.\\d+\\.\\d+",
    MatchOperation.REGEX,
    false
);

// Match email in custom header
Condition emailPattern = new HeaderCondition(
    "X-User-Email",
    "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
    MatchOperation.REGEX,
    false
);

// Match UUID format
Condition uuidPattern = new HeaderCondition(
    "X-Request-Id",
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    MatchOperation.REGEX,
    false
);

ConditionResult result = matcher.evaluate(versionPattern, request);
```

### Evaluation Modes

#### FAIL_FAST Mode (Default)

Stops at the first failure for better performance:

```java
Condition condition = ConditionGroup.builder()
    .and(new HeaderCondition("X-Key-1", "value1", MatchOperation.EQUALS, false))
    .and(new HeaderCondition("X-Key-2", "value2", MatchOperation.EQUALS, false))
    .and(new HeaderCondition("X-Key-3", "value3", MatchOperation.EQUALS, false))
    .mode(EvaluationMode.FAIL_FAST)  // Stop at first failure
    .build();

ConditionResult result = matcher.evaluate(condition, request);
// If X-Key-1 fails, X-Key-2 and X-Key-3 are not evaluated
// result.getFailures() contains only the first failure
```

#### COLLECT_ALL Mode

Evaluates all conditions and collects all failures for debugging:

```java
Condition condition = ConditionGroup.builder()
    .and(new HeaderCondition("X-Key-1", "value1", MatchOperation.EQUALS, false))
    .and(new HeaderCondition("X-Key-2", "value2", MatchOperation.EQUALS, false))
    .and(new HeaderCondition("X-Key-3", "value3", MatchOperation.EQUALS, false))
    .mode(EvaluationMode.COLLECT_ALL)  // Evaluate all conditions
    .build();

ConditionResult result = matcher.evaluate(condition, request);
// All three conditions are evaluated even if X-Key-1 fails
// result.getFailures() contains all failures for detailed debugging

if (!result.isMatched()) {
    System.out.println("Found " + result.getFailures().size() + " failures:");
    for (ConditionFailure failure : result.getFailures()) {
        System.out.println("  - " + failure.getMessage());
    }
}
```

### Custom Conditions

Implement business logic with custom conditions:

```java
import com.cleveloper.jufu.requestutils.condition.core.*;

public class WorkingHoursCondition implements Condition {

    @Override
    public ConditionResult evaluate(RequestContext context) {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        if (now.isAfter(start) && now.isBefore(end)) {
            return ConditionResult.success();
        }

        return ConditionResult.failure(
            ConditionFailure.builder()
                .conditionType("WorkingHours")
                .fieldName("current-time")
                .operation("between")
                .expectedValue("09:00-17:00")
                .actualValue(now.toString())
                .message("Request received outside working hours")
                .build()
        );
    }
}

// Use with other conditions
Condition businessHoursCheck = ConditionGroup.and(
    new HeaderCondition("X-Api-Key", "valid", MatchOperation.EQUALS, false),
    new WorkingHoursCondition()
);

ConditionResult result = matcher.evaluate(businessHoursCheck, request);
```

### Annotation-Based (AOP) Usage

Simplify condition matching with declarative annotations. The `@JUFUMatchConditions` annotation allows you to define conditions directly on controller methods.

#### Enable AOP Support

AOP support is automatically enabled with Spring Boot auto-configuration. No additional setup required!

#### Basic Header Annotation

```java
import com.cleveloper.jufu.requestutils.condition.annotations.*;

@RestController
public class ApiController {

    @GetMapping("/premium/users")
    @JUFUMatchConditions(value = {
        @JUFUCondition(header = @JUFUHeader(
            name = "X-User-Type",
            equals = "premium"
        ))
    })
    public ResponseEntity<?> getPremiumUsers() {
        // Only called if X-User-Type header equals "premium"
        return ResponseEntity.ok(premiumUsers);
    }
}
```

#### Query Parameter Annotation

```java
@GetMapping("/api/data")
@JUFUMatchConditions(value = {
    @JUFUCondition(queryParam = @JUFUQueryParam(
        name = "version",
        equals = "v2"
    ))
})
public ResponseEntity<?> getDataV2() {
    // Only called if version parameter equals "v2"
    return ResponseEntity.ok(data);
}
```

#### JSON Path Matching

Match against JSON request body fields using JSONPath:

```java
@PostMapping("/api/users")
@JUFUMatchConditions(value = {
    @JUFUCondition(jsonPath = @JUFUJsonPath(
        path = "$.user.role",
        equals = "admin"
    ))
})
public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
    // Only called if JSON body contains {"user": {"role": "admin"}}
    return ResponseEntity.ok("User created");
}
```

#### Multiple Conditions (AND Logic)

All conditions must be met:

```java
@GetMapping("/api/secure/data")
@JUFUMatchConditions(
    value = {
        @JUFUCondition(header = @JUFUHeader(
            name = "X-API-Key",
            equals = "valid-key"
        )),
        @JUFUCondition(queryParam = @JUFUQueryParam(
            name = "version",
            startsWith = "v2"
        ))
    },
    mode = EvaluationMode.FAIL_FAST
)
public ResponseEntity<?> getSecureData() {
    // Only called if BOTH conditions are met
    return ResponseEntity.ok(data);
}
```

#### String Matching Operations

All string matching operations are supported:

```java
// Exact match
@JUFUCondition(header = @JUFUHeader(name = "X-Type", equals = "premium"))

// Contains substring
@JUFUCondition(header = @JUFUHeader(name = "User-Agent", contains = "Mobile"))

// Starts with prefix
@JUFUCondition(header = @JUFUHeader(name = "Authorization", startsWith = "Bearer "))

// Ends with suffix
@JUFUCondition(queryParam = @JUFUQueryParam(name = "format", endsWith = "json"))

// Regex pattern
@JUFUCondition(header = @JUFUHeader(
    name = "X-Request-Id",
    regex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
))

// Case-insensitive matching
@JUFUCondition(header = @JUFUHeader(
    name = "Content-Type",
    equals = "application/json",
    ignoreCase = true
))
```

#### Exception Handling with AOP

When conditions fail, a `ConditionNotMetException` is thrown. Handle it with `@ControllerAdvice`:

```java
import com.cleveloper.jufu.requestutils.condition.aop.ConditionNotMetException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@ControllerAdvice
public class ConditionExceptionHandler {

    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<ErrorResponse> handleConditionNotMet(
            ConditionNotMetException ex) {

        ErrorResponse error = new ErrorResponse(
            "FORBIDDEN",
            "Condition not met: " + ex.getMessage(),
            ex.getResult().getFailures()
        );

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(error);
    }
}
```

#### Real-World Example: Secured API Endpoint

```java
@RestController
@RequestMapping("/api/v2")
public class SecuredApiController {

    @PostMapping("/admin/users")
    @JUFUMatchConditions(
        value = {
            // Validate API key header
            @JUFUCondition(header = @JUFUHeader(
                name = "X-API-Key",
                regex = "^[A-Za-z0-9]{32}$"
            )),
            // Ensure JSON content type
            @JUFUCondition(header = @JUFUHeader(
                name = "Content-Type",
                equals = "application/json",
                ignoreCase = true
            )),
            // Check user role in JSON body
            @JUFUCondition(jsonPath = @JUFUJsonPath(
                path = "$.requestor.role",
                equals = "admin"
            ))
        },
        mode = EvaluationMode.COLLECT_ALL  // Get all validation errors
    )
    public ResponseEntity<?> createAdminUser(@RequestBody CreateUserRequest request) {
        // This method only executes if:
        // 1. X-API-Key header is valid
        // 2. Content-Type is application/json
        // 3. JSON body has requestor.role = "admin"

        // Business logic here
        return ResponseEntity.ok("User created successfully");
    }
}
```

Request example that would pass:
```bash
curl -X POST https://api.example.com/api/v2/admin/users \
  -H "X-API-Key: abc123def456ghi789jkl012mno345pq" \
  -H "Content-Type: application/json" \
  -d '{
    "requestor": {
      "role": "admin"
    },
    "newUser": {
      "name": "John Doe",
      "email": "john@example.com"
    }
  }'
```

#### Advantages of AOP Approach

1. **Declarative**: Conditions are visible in method signatures
2. **Reusable**: Same annotations across multiple endpoints
3. **Maintainable**: Less boilerplate code
4. **Testable**: Easy to mock and test in isolation
5. **Clean Separation**: Business logic separated from validation

## Exception Handling

### ConditionNotMetException

Thrown when conditions fail:

```java
import com.cleveloper.jufu.requestutils.condition.aop.ConditionNotMetException;

public void processRequest(HttpServletRequest request) {
    Condition condition = new HeaderCondition(
        "X-Api-Key",
        "secret",
        MatchOperation.EQUALS,
        false
    );

    ConditionResult result = matcher.evaluate(condition, request);

    if (!result.isMatched()) {
        throw new ConditionNotMetException(result);
    }

    // Process request...
}
```

### Global Exception Handler

Create a `@ControllerAdvice` to handle exceptions globally:

```java
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<ErrorResponse> handleConditionNotMet(
            ConditionNotMetException ex) {

        ErrorResponse error = new ErrorResponse(
            "CONDITION_NOT_MET",
            ex.getMessage(),
            ex.getResult().getFailures()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
}

class ErrorResponse {
    private String code;
    private String message;
    private List<ConditionFailure> failures;

    // Constructors, getters, setters
}
```

### Exception Message Format

Single failure:
```
Condition not met: Header 'X-Api-Key' expected to equal 'valid-key' but was 'invalid-key'
```

Multiple failures (COLLECT_ALL mode):
```
Conditions not met (2 failures):
  - Header 'X-Api-Key' expected to equal 'valid-key' but was 'invalid-key'
  - QueryParam 'version' expected to start with 'v2' but was 'v1.0'
```

## Configuration

### Spring Boot Auto-Configuration

Zero configuration required! The `RequestConditionMatcher` is automatically registered as a Spring bean.

### Application Properties

Currently no configuration properties are needed. The service works out of the box.

**Coming soon:**
```properties
# Global evaluation mode (default: FAIL_FAST)
jufu.condition-matcher.evaluation-mode=FAIL_FAST

# Enable/disable AOP aspect (default: true)
jufu.condition-matcher.aop.enabled=true

# AOP aspect order (default: 100)
jufu.condition-matcher.aop.order=100
```

## API Reference

### Core Classes

#### RequestConditionMatcher

Main service for evaluating conditions.

```java
public class RequestConditionMatcher {
    ConditionResult evaluate(Condition condition, HttpServletRequest request);
    ConditionResult evaluate(Condition condition, RequestContext context);
}
```

#### Condition Interface

```java
@FunctionalInterface
public interface Condition {
    ConditionResult evaluate(RequestContext context);
}
```

#### HeaderCondition

```java
public class HeaderCondition implements Condition {
    public HeaderCondition(
        String headerName,
        String expectedValue,
        MatchOperation operation,
        boolean ignoreCase
    );
}
```

#### QueryParamCondition

```java
public class QueryParamCondition implements Condition {
    public QueryParamCondition(
        String paramName,
        String expectedValue,
        MatchOperation operation,
        boolean ignoreCase
    );
}
```

#### ConditionGroup

```java
public class ConditionGroup implements Condition {
    static ConditionGroup and(Condition... conditions);
    static ConditionGroup or(Condition... conditions);
    static ConditionGroupBuilder builder();
}
```

#### ConditionGroupBuilder

```java
public class ConditionGroupBuilder {
    ConditionGroupBuilder and(Condition condition);
    ConditionGroupBuilder or(Condition condition);
    ConditionGroupBuilder andGroup(Consumer<ConditionGroupBuilder> groupBuilder);
    ConditionGroupBuilder orGroup(Consumer<ConditionGroupBuilder> groupBuilder);
    ConditionGroupBuilder mode(EvaluationMode mode);
    Condition build();
}
```

## Examples

### Example 1: API Versioning

Route requests based on API version:

```java
@RestController
public class ApiController {

    @Autowired
    private RequestConditionMatcher matcher;

    @GetMapping("/api/users")
    public ResponseEntity<?> getUsers(HttpServletRequest request) {
        // Check for v2 API
        Condition v2Check = new QueryParamCondition(
            "version",
            "v2",
            MatchOperation.STARTS_WITH,
            false
        );

        if (matcher.evaluate(v2Check, request).isMatched()) {
            return getUsersV2();
        } else {
            return getUsersV1();
        }
    }

    private ResponseEntity<?> getUsersV1() { /* ... */ }
    private ResponseEntity<?> getUsersV2() { /* ... */ }
}
```

### Example 2: Multi-Tenancy Routing

Route requests based on tenant headers:

```java
@Service
public class TenantRouter {

    @Autowired
    private RequestConditionMatcher matcher;

    public DataSource getDataSource(HttpServletRequest request) {
        // Premium tenant
        Condition premiumTenant = ConditionGroup.and(
            new HeaderCondition("X-Tenant-Type", "premium", MatchOperation.EQUALS, false),
            new HeaderCondition("X-Tenant-Id", "^PREM-\\d+$", MatchOperation.REGEX, false)
        );

        if (matcher.evaluate(premiumTenant, request).isMatched()) {
            return premiumDataSource;
        }

        // Enterprise tenant
        Condition enterpriseTenant = ConditionGroup.and(
            new HeaderCondition("X-Tenant-Type", "enterprise", MatchOperation.EQUALS, false),
            new HeaderCondition("X-Tenant-Id", "^ENT-\\d+$", MatchOperation.REGEX, false)
        );

        if (matcher.evaluate(enterpriseTenant, request).isMatched()) {
            return enterpriseDataSource;
        }

        return standardDataSource;
    }
}
```

### Example 3: Feature Flags

Enable features based on request parameters:

```java
@Service
public class FeatureService {

    @Autowired
    private RequestConditionMatcher matcher;

    public boolean isBetaFeatureEnabled(HttpServletRequest request) {
        Condition betaFeature = ConditionGroup.or(
            // Beta flag in query param
            new QueryParamCondition("beta", "true", MatchOperation.EQUALS, false),
            // Beta user header
            new HeaderCondition("X-User-Beta", "enabled", MatchOperation.EQUALS, false),
            // Internal request
            new HeaderCondition("X-Internal-Request", "true", MatchOperation.EQUALS, false)
        );

        return matcher.evaluate(betaFeature, request).isMatched();
    }
}
```

### Example 4: Request Validation

Validate API requests:

```java
@RestController
public class SecureApiController {

    @Autowired
    private RequestConditionMatcher matcher;

    @PostMapping("/api/secure/data")
    public ResponseEntity<?> processData(HttpServletRequest request) {
        // Validate required headers
        Condition validation = ConditionGroup.builder()
            .and(new HeaderCondition("X-Api-Key", "^[A-Za-z0-9]{32}$", MatchOperation.REGEX, false))
            .and(new HeaderCondition("Content-Type", "application/json", MatchOperation.EQUALS, true))
            .and(new HeaderCondition("X-Request-Id", "^[0-9a-f-]{36}$", MatchOperation.REGEX, false))
            .mode(EvaluationMode.COLLECT_ALL)  // Get all validation errors
            .build();

        ConditionResult result = matcher.evaluate(validation, request);

        if (!result.isMatched()) {
            // Return all validation errors
            Map<String, Object> errors = new HashMap<>();
            errors.put("message", "Request validation failed");
            errors.put("failures", result.getFailures());

            return ResponseEntity
                .badRequest()
                .body(errors);
        }

        // Process valid request
        return ResponseEntity.ok("Data processed");
    }
}
```

### Example 5: A/B Testing

Route requests for A/B testing:

```java
@Service
public class ABTestingService {

    @Autowired
    private RequestConditionMatcher matcher;

    public String getVariant(HttpServletRequest request) {
        // Variant A: Users with experiment header
        Condition variantA = new HeaderCondition(
            "X-Experiment",
            "variant-a",
            MatchOperation.EQUALS,
            false
        );

        if (matcher.evaluate(variantA, request).isMatched()) {
            return "A";
        }

        // Variant B: Users with experiment parameter
        Condition variantB = new QueryParamCondition(
            "experiment",
            "variant-b",
            MatchOperation.EQUALS,
            false
        );

        if (matcher.evaluate(variantB, request).isMatched()) {
            return "B";
        }

        // Default variant
        return "CONTROL";
    }
}
```

### Example 6: Mobile vs Desktop Routing

Route based on device type:

```java
@Service
public class DeviceRouter {

    @Autowired
    private RequestConditionMatcher matcher;

    public boolean isMobileRequest(HttpServletRequest request) {
        Condition mobileDevice = ConditionGroup.or(
            new HeaderCondition("User-Agent", "Mobile", MatchOperation.CONTAINS, true),
            new HeaderCondition("User-Agent", "Android", MatchOperation.CONTAINS, true),
            new HeaderCondition("User-Agent", "iPhone", MatchOperation.CONTAINS, true),
            new HeaderCondition("User-Agent", "iPad", MatchOperation.CONTAINS, true)
        );

        return matcher.evaluate(mobileDevice, request).isMatched();
    }

    public String getTemplate(HttpServletRequest request) {
        return isMobileRequest(request) ? "mobile/template" : "desktop/template";
    }
}
```

## Roadmap

### Phase 1 (Completed) ✅
- Core condition matching engine
- Header and query parameter matching
- String operations (equals, contains, starts with, ends with, regex)
- AND/OR grouping with nesting
- Evaluation modes (FAIL_FAST, COLLECT_ALL)
- Builder API
- Spring Boot auto-configuration

### Phase 2 (Completed) ✅
- JSON payload matching
  - JSONPath-based field extraction and value matching
  - Exact field matching with template comparison
- Annotation-based AOP integration
  - `@JUFUMatchConditions` annotation for declarative conditions
  - Method-level condition declarations
  - Inline condition definitions (`@JUFUHeader`, `@JUFUQueryParam`, `@JUFUJsonPath`, `@JUFUJsonExactMatch`)
  - Automatic aspect execution with Spring AOP
  - `ConditionNotMetException` for failed conditions

### Phase 3 (Planned) 📋
- Configuration properties for global settings
  - Global evaluation mode configuration
  - AOP aspect order customization
  - Enable/disable AOP integration
- Additional condition types
  - Rate limiting conditions
  - IP address matching
  - Time-based conditions (working hours, date ranges)
  - Request body size validation
  - HTTP method matching
- Performance optimizations
  - Condition result caching
  - Compiled regex pattern caching
- Observability
  - Metrics and monitoring integration (Micrometer)
  - Condition evaluation tracing
  - Performance insights

## Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'feat: add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Setup

```bash
# Clone the repository
git clone https://github.com/abudhahir/java-utility-functions.git

# Navigate to request-utils
cd java-utility-functions/request-utils

# Build and test
./mvnw clean install

# Run tests
./mvnw test
```

### Testing

All features must include comprehensive tests:

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=HeaderConditionTest

# Run with coverage
./mvnw clean test jacoco:report
```

## License

[Specify your license here]

## Support

For questions, issues, or feature requests:
- **Issues**: https://github.com/abudhahir/java-utility-functions/issues
- **Discussions**: https://github.com/abudhahir/java-utility-functions/discussions

---

**Version:** 1.0.0-SNAPSHOT
**Last Updated:** 2026-03-09
**Maintainer:** [Your name/organization]
