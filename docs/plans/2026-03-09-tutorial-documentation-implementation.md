# Getting Started Tutorial Documentation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create comprehensive multi-file tutorial documentation for request-utils module with progressive learning paths

**Architecture:** 10 markdown files organized as progressive tutorial (index → quick start → concepts → advanced → examples → troubleshooting), targeting Spring Boot developers with copy-paste ready code and deep explanations

**Tech Stack:** Markdown documentation, Java 17 code examples, Spring Boot 4.0.3, Maven

---

## Task 1: Create Directory Structure

**Files:**
- Create: `request-utils/docs/tutorial/` directory
- Create: `request-utils/docs/tutorial/examples/` directory

**Step 1: Create tutorial directory**

```bash
cd request-utils
mkdir -p docs/tutorial/examples
```

**Step 2: Verify structure**

Run: `ls -la docs/tutorial/`
Expected: Directory exists with examples subdirectory

**Step 3: Commit**

```bash
git add docs/tutorial/.gitkeep docs/tutorial/examples/.gitkeep
git commit -m "docs: create tutorial directory structure"
```

---

## Task 2: Index Page (00-index.md)

**Files:**
- Create: `request-utils/docs/tutorial/00-index.md`

**Step 1: Write index page content**

Create: `request-utils/docs/tutorial/00-index.md`

```markdown
# Request Utils Tutorial

**Master Request Condition Matching in Spring Boot**

Welcome to the comprehensive tutorial for request-utils, a powerful Spring Boot utility for HTTP request condition matching. This tutorial will take you from zero to production-ready request routing, validation, and filtering.

---

## What is request-utils?

Request-utils provides a flexible condition matching engine for HTTP requests in Spring Boot applications. Evaluate headers, query parameters, and JSON payloads against configured conditions with detailed success/failure reporting.

**Perfect for:**
- API versioning and routing
- Multi-tenancy request handling
- Feature flag implementation
- Request validation and filtering
- A/B testing and canary deployments

---

## Who is This For?

This tutorial is designed for **Spring Boot developers** who want to add sophisticated request matching to their applications. We assume you're comfortable with:
- Java 17+
- Spring Boot 4.0.3+ basics
- Dependency injection
- REST API fundamentals

---

## What You'll Learn

- ✅ **Basic matching** (15 min) - Headers and query parameters
- ✅ **Complex conditions** (20 min) - AND/OR logic, nested groups
- ✅ **JSON matching** (15 min) - JSONPath and exact field matching
- ✅ **AOP integration** (20 min) - Declarative annotations
- ✅ **Custom conditions** (25 min) - Business logic integration
- ✅ **Complete examples** (30 min) - Real-world applications
- ✅ **Troubleshooting** (15 min) - Debug and optimize

**Total time:** 2.5 - 4 hours depending on depth

---

## Learning Paths

Choose the path that fits your needs:

### 🚀 Fast Track (30 minutes)
**Goal:** Get working code quickly
**Path:** 01 Quick Start → 08 Complete Examples
**Best for:** Learning by example, need immediate results

### 📚 Comprehensive (90 minutes)
**Goal:** Core feature mastery
**Path:** 01 → 02 → 03 → 04 → 08
**Best for:** Understanding fundamentals, production usage

### 🎓 Full Course (3-4 hours)
**Goal:** Complete understanding
**Path:** Sequential 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09
**Best for:** Team training, architecture decisions

### 🔧 Problem-Driven (variable)
**Goal:** Fix specific issues
**Path:** Start at 09 Troubleshooting → Jump to relevant sections
**Best for:** Debugging existing implementations

---

## Tutorial Contents

| # | Topic | Level | Time | Description |
|---|-------|-------|------|-------------|
| [01](01-quick-start.md) | Quick Start | Beginner | 10 min | Get your first condition working |
| [02](02-core-concepts.md) | Core Concepts | Beginner | 20 min | Understanding the architecture |
| [03](03-headers-and-params.md) | Headers & Params | Intermediate | 25 min | Practical matching scenarios |
| [04](04-building-complex-conditions.md) | Complex Conditions | Intermediate | 25 min | AND/OR logic and nesting |
| [05](05-json-matching.md) | JSON Matching | Advanced | 20 min | JSONPath and exact matching |
| [06](06-aop-annotations.md) | AOP Integration | Advanced | 25 min | Declarative annotations |
| [07](07-custom-conditions.md) | Custom Conditions | Advanced | 30 min | Build your own logic |
| [08](08-complete-examples.md) | Complete Examples | All Levels | 45 min | Real-world applications |
| [09](09-troubleshooting.md) | Troubleshooting | All Levels | 20 min | Debug and optimize |

---

## Prerequisites

Before starting, ensure you have:

- ✅ Java 17 or higher installed
- ✅ Spring Boot 4.0.3+ project setup
- ✅ Maven or Gradle configured
- ✅ IDE of your choice (IntelliJ IDEA, VS Code, Eclipse)
- ✅ Basic understanding of REST APIs
- ✅ Familiarity with Spring dependency injection

**Not sure?** Check the [Spring Boot Getting Started Guide](https://spring.io/guides/gs/spring-boot/)

---

## Additional Resources

**API Reference:**
- [README](../../README.md) - Complete API documentation
- [JavaDoc](../javadoc/) - Detailed class documentation

**Code Examples:**
- [examples/](examples/) - Runnable code samples
- [GitHub Repository](https://github.com/abudhahir/java-utility-functions) - Full source code

**Support:**
- [GitHub Issues](https://github.com/abudhahir/java-utility-functions/issues) - Bug reports and feature requests
- [GitHub Discussions](https://github.com/abudhahir/java-utility-functions/discussions) - Questions and community

---

## Getting Started

Ready to begin? **[Start with Quick Start →](01-quick-start.md)**

Already familiar with the basics? **[Jump to Complete Examples →](08-complete-examples.md)**

Having issues? **[Check Troubleshooting →](09-troubleshooting.md)**

---

**Last Updated:** 2026-03-09
**Version:** 1.0.0-SNAPSHOT
```

**Step 2: Verify formatting**

Run: `cat request-utils/docs/tutorial/00-index.md | head -20`
Expected: File created with proper markdown formatting

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/00-index.md
git commit -m "docs(tutorial): add index page with learning paths"
```

---

## Task 3: Quick Start Guide (01-quick-start.md)

**Files:**
- Create: `request-utils/docs/tutorial/01-quick-start.md`

**Step 1: Write quick start content**

Create: `request-utils/docs/tutorial/01-quick-start.md`

```markdown
# Quick Start

**Goal:** Get your first request condition working in 10 minutes.

This guide gets you up and running with minimal explanation. Want to understand the details? See [Core Concepts](02-core-concepts.md) after completing this guide.

---

## What We're Building

A simple API endpoint that routes premium users to special features based on a header value.

**Input:** HTTP request with `X-User-Type: premium` header
**Output:** Request allowed or blocked based on condition

---

## Step 1: Add Dependency

Add request-utils to your `pom.xml`:

```xml
<dependency>
    <groupId>com.cleveloper.jufu</groupId>
    <artifactId>request-utils</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**That's it!** Auto-configuration handles everything else.

---

## Step 2: Inject the Matcher

Create a controller and inject `RequestConditionMatcher`:

```java
package com.example.demo;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public UserController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    // We'll add methods next...
}
```

---

## Step 3: Create Your First Condition

Add a method that checks for premium users:

```java
@GetMapping("/feature")
public ResponseEntity<String> premiumFeature(HttpServletRequest request) {
    // Create condition: header "X-User-Type" must equal "premium"
    Condition condition = new HeaderCondition(
        "X-User-Type",           // Header name
        "premium",               // Expected value
        MatchOperation.EQUALS,   // Match operation
        false                    // Case sensitive
    );

    // Evaluate the condition
    ConditionResult result = matcher.evaluate(condition, request);

    // Check if condition matched
    if (result.isMatched()) {
        return ResponseEntity.ok("Welcome, premium user!");
    } else {
        return ResponseEntity.status(403)
            .body("Premium users only");
    }
}
```

---

## Step 4: Test It

Start your application and test with curl:

**Premium user (allowed):**
```bash
curl -H "X-User-Type: premium" http://localhost:8080/api/feature
# Response: Welcome, premium user!
```

**Regular user (blocked):**
```bash
curl -H "X-User-Type: basic" http://localhost:8080/api/feature
# Response: Premium users only (HTTP 403)
```

**No header (blocked):**
```bash
curl http://localhost:8080/api/feature
# Response: Premium users only (HTTP 403)
```

---

## 🎉 Success!

You've just implemented request condition matching! The condition checked the header and allowed or blocked the request accordingly.

---

## What Just Happened?

1. **HeaderCondition** - Checked if `X-User-Type` header equals `premium`
2. **RequestConditionMatcher** - Evaluated the condition against the incoming request
3. **ConditionResult** - Returned success or failure based on the match

---

## See the Failures

Want to see why a condition failed? Check the failure details:

```java
if (!result.isMatched()) {
    // Get detailed failure information
    for (ConditionFailure failure : result.getFailures()) {
        System.out.println(failure.getMessage());
        // Output: "Header 'X-User-Type' expected to equal 'premium' but was 'basic'"
    }

    return ResponseEntity.status(403)
        .body("Premium users only");
}
```

---

## Next Steps

**Want to understand the architecture?**
→ [Core Concepts](02-core-concepts.md) - Learn about Condition, ConditionResult, and evaluation modes

**Ready for more practical examples?**
→ [Headers and Params](03-headers-and-params.md) - Real-world matching scenarios

**Need multiple conditions?**
→ [Complex Conditions](04-building-complex-conditions.md) - AND/OR logic and nesting

**Prefer declarative style?**
→ [AOP Annotations](06-aop-annotations.md) - Use `@JUFUMatchConditions` annotation

---

## Common Questions

**Q: Does it work with query parameters?**
A: Yes! Use `QueryParamCondition` instead of `HeaderCondition`. Same API.

**Q: Can I check multiple conditions?**
A: Yes! See [Complex Conditions](04-building-complex-conditions.md) for AND/OR logic.

**Q: Can I match JSON payloads?**
A: Yes! See [JSON Matching](05-json-matching.md) for JSONPath support.

**Q: Is auto-configuration required?**
A: No, but it's convenient. You can instantiate `RequestConditionMatcher` manually if needed.

---

**[← Back to Index](00-index.md)** | **[Next: Core Concepts →](02-core-concepts.md)**
```

**Step 2: Verify content**

Run: `wc -w request-utils/docs/tutorial/01-quick-start.md`
Expected: Approximately 500-700 words

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/01-quick-start.md
git commit -m "docs(tutorial): add quick start guide with working example"
```

---

## Task 4: Core Concepts (02-core-concepts.md)

**Files:**
- Create: `request-utils/docs/tutorial/02-core-concepts.md`

**Step 1: Write core concepts content**

Create: `request-utils/docs/tutorial/02-core-concepts.md`

```markdown
# Core Concepts

**Goal:** Understand the architecture and key components of request condition matching.

This guide explains the fundamental building blocks. After reading, you'll know when to use each component and how they work together.

---

## Architecture Overview

Request condition matching follows a simple flow:

```
HTTP Request → RequestContext → Condition → ConditionResult
                                    ↓
                            matched or failures
```

**Key insight:** Conditions are pure functions that take a `RequestContext` and return a `ConditionResult`. This makes them composable, testable, and reusable.

---

## The Four Key Interfaces

### 1. Condition

The core abstraction - a function that evaluates to true or false:

```java
@FunctionalInterface
public interface Condition {
    ConditionResult evaluate(RequestContext context);
}
```

**Built-in implementations:**
- `HeaderCondition` - Match HTTP headers
- `QueryParamCondition` - Match query parameters
- `JsonPathCondition` - Extract and match JSON fields
- `JsonExactMatchCondition` - Match specific JSON fields
- `ConditionGroup` - Combine conditions with AND/OR logic

**Key property:** Conditions are stateless and immutable. Create once, reuse everywhere.

```java
// Create once
private static final Condition PREMIUM_USER = new HeaderCondition(
    "X-User-Type", "premium", MatchOperation.EQUALS, false
);

// Reuse in multiple methods
public void method1(HttpServletRequest request) {
    ConditionResult result = matcher.evaluate(PREMIUM_USER, request);
}

public void method2(HttpServletRequest request) {
    ConditionResult result = matcher.evaluate(PREMIUM_USER, request);
}
```

---

### 2. ConditionResult

The outcome of evaluating a condition:

```java
public class ConditionResult {
    boolean isMatched();                    // Did all conditions pass?
    List<ConditionFailure> getFailures();   // Why did it fail? (empty if matched)
}
```

**Usage pattern:**

```java
ConditionResult result = matcher.evaluate(condition, request);

if (result.isMatched()) {
    // Success path
} else {
    // Failure path - examine failures for details
    for (ConditionFailure failure : result.getFailures()) {
        log.warn("Condition failed: {}", failure.getMessage());
    }
}
```

**Key insight:** `ConditionResult` never throws exceptions. It captures success or failure as data, making error handling explicit and composable.

---

### 3. ConditionFailure

Detailed information about why a condition failed:

```java
public class ConditionFailure {
    String getConditionType();    // "Header", "QueryParam", "JsonPath"
    String getFieldName();        // "X-Api-Key", "version", "$.user.email"
    String getOperation();        // "equals", "contains", "starts with"
    String getExpectedValue();    // "premium", "v2", "admin"
    String getActualValue();      // "basic", "v1", "[not present]"
    String getMessage();          // Human-readable description
}
```

**Example failure message:**
```
Header 'X-User-Type' expected to equal 'premium' but was 'basic'
```

**Why this matters:**
- Debugging: See exactly why a request was rejected
- Error responses: Return structured failures to clients
- Logging: Capture detailed audit trails

```java
if (!result.isMatched()) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("message", "Request validation failed");
    errorResponse.put("failures", result.getFailures().stream()
        .map(ConditionFailure::getMessage)
        .collect(Collectors.toList()));

    return ResponseEntity.badRequest().body(errorResponse);
}
```

---

### 4. RequestContext

Provides access to request data in a testable way:

```java
public interface RequestContext {
    String getHeader(String name);
    String getQueryParam(String name);
    Object getJsonBody();

    // Factory method
    static RequestContext from(HttpServletRequest request);
}
```

**Why context?** Decouples conditions from `HttpServletRequest`, making them:
- **Testable:** Mock `RequestContext` instead of servlet request
- **Flexible:** Use outside web layer if needed
- **Cacheable:** Request body parsed once, reused for multiple conditions

**Usage in custom conditions:**

```java
public class CustomCondition implements Condition {
    @Override
    public ConditionResult evaluate(RequestContext context) {
        String apiKey = context.getHeader("X-Api-Key");
        // ... evaluate logic
    }
}
```

---

## Match Operations

Five operations for string matching:

```java
public enum MatchOperation {
    EQUALS,        // Exact match: "premium" == "premium"
    CONTAINS,      // Substring: "Mobile" in "Mozilla/5.0 (Mobile)"
    STARTS_WITH,   // Prefix: "Bearer " at start of "Bearer token123"
    ENDS_WITH,     // Suffix: "/json" at end of "application/json"
    REGEX          // Pattern: "v\\d+\\.\\d+" matches "v2.0", "v3.5"
}
```

**When to use each:**

| Operation | Use Case | Example |
|-----------|----------|---------|
| `EQUALS` | Exact value match | API key validation, tenant ID |
| `CONTAINS` | Partial match | User-Agent detection, search |
| `STARTS_WITH` | Prefix match | Authorization headers, versioning |
| `ENDS_WITH` | Suffix match | Content-Type checking, file extensions |
| `REGEX` | Pattern match | Email validation, complex formats |

**Performance tip:** `EQUALS` > `STARTS_WITH`/`ENDS_WITH` > `CONTAINS` > `REGEX`. Use the simplest operation that works.

---

## Evaluation Modes

Control how conditions are evaluated:

```java
public enum EvaluationMode {
    FAIL_FAST,     // Stop at first failure (default)
    COLLECT_ALL    // Evaluate all, collect all failures
}
```

### FAIL_FAST Mode (Default)

**When to use:** Production, performance-critical paths

**Behavior:** Stops at the first failure

```java
Condition condition = ConditionGroup.builder()
    .and(condition1)  // If this fails...
    .and(condition2)  // ...this is never evaluated
    .and(condition3)
    .mode(EvaluationMode.FAIL_FAST)
    .build();
```

**Advantages:**
- ⚡ Faster (skips unnecessary checks)
- 🎯 Sufficient for most use cases
- 💰 Cheaper (fewer operations)

**Disadvantage:**
- Only see first failure (harder to debug multiple issues)

---

### COLLECT_ALL Mode

**When to use:** Development, debugging, detailed error reporting

**Behavior:** Evaluates all conditions, collects all failures

```java
Condition condition = ConditionGroup.builder()
    .and(condition1)  // Evaluated
    .and(condition2)  // Evaluated even if condition1 fails
    .and(condition3)  // Evaluated even if condition1 & 2 fail
    .mode(EvaluationMode.COLLECT_ALL)
    .build();
```

**Advantages:**
- 🔍 See all failures at once
- 🐛 Better debugging experience
- 📋 Complete validation reports

**Disadvantage:**
- 🐌 Slower (evaluates everything)

**Example: Form validation**

```java
// Check all required fields, report all missing ones
Condition formValidation = ConditionGroup.builder()
    .and(new HeaderCondition("Content-Type", "application/json", MatchOperation.EQUALS, true))
    .and(new JsonPathCondition("$.email", ".+@.+", MatchOperation.REGEX, false))
    .and(new JsonPathCondition("$.name", ".{3,}", MatchOperation.REGEX, false))
    .mode(EvaluationMode.COLLECT_ALL)  // Show all validation errors
    .build();

ConditionResult result = matcher.evaluate(formValidation, request);
if (!result.isMatched()) {
    // Return all validation errors to user
    return ResponseEntity.badRequest().body(result.getFailures());
}
```

---

## Design Philosophy

Understanding the "why" behind the design:

### 1. Functional Interfaces

**Why:** Composability and flexibility

```java
// Conditions are functions
Condition c1 = ctx -> ...;
Condition c2 = ctx -> ...;

// Easily combine
Condition combined = ConditionGroup.and(c1, c2);
```

### 2. Immutability

**Why:** Thread safety and predictability

```java
// Create once
Condition condition = new HeaderCondition(...);

// Use anywhere, anytime - always safe
executor.submit(() -> matcher.evaluate(condition, request1));
executor.submit(() -> matcher.evaluate(condition, request2));
```

### 3. Explicit Failures

**Why:** No hidden control flow, better debugging

```java
// Bad: Exceptions for control flow
try {
    validateRequest(request);
} catch (InvalidHeaderException e) {
    // Exception handling mixes errors with business logic
}

// Good: Explicit success/failure
ConditionResult result = matcher.evaluate(condition, request);
if (!result.isMatched()) {
    // Clear business logic
}
```

### 4. Separation of Concerns

**Why:** Testability and flexibility

- **Condition:** What to check (pure logic)
- **RequestContext:** Where to get data (abstraction)
- **ConditionResult:** What happened (data)
- **RequestConditionMatcher:** How to evaluate (execution)

Each component has one job and does it well.

---

## Putting It Together

Here's how all components work together:

```java
// 1. Define what to check (Condition)
Condition condition = new HeaderCondition(
    "X-Api-Key",
    "secret",
    MatchOperation.EQUALS,
    false
);

// 2. Create context from request (RequestContext)
RequestContext context = RequestContext.from(request);

// 3. Evaluate (RequestConditionMatcher)
ConditionResult result = matcher.evaluate(condition, context);

// 4. Act on result (ConditionResult + ConditionFailure)
if (result.isMatched()) {
    // Success
} else {
    // Examine failures
    result.getFailures().forEach(failure -> {
        System.out.println(failure.getConditionType());  // "Header"
        System.out.println(failure.getFieldName());      // "X-Api-Key"
        System.out.println(failure.getOperation());      // "equals"
        System.out.println(failure.getExpectedValue());  // "secret"
        System.out.println(failure.getActualValue());    // "wrong-key"
        System.out.println(failure.getMessage());        // Full message
    });
}
```

---

## Key Takeaways

1. **Conditions are functions** - Create once, reuse everywhere
2. **Results are data** - No exceptions for control flow
3. **Failures are detailed** - Always know why something didn't match
4. **Context is abstract** - Testable and flexible
5. **Operations have tradeoffs** - Choose the simplest that works
6. **Modes have purposes** - FAIL_FAST for production, COLLECT_ALL for debugging

---

## Next Steps

**Ready for practical examples?**
→ [Headers and Params](03-headers-and-params.md) - Common matching scenarios

**Want to combine conditions?**
→ [Complex Conditions](04-building-complex-conditions.md) - AND/OR logic

**Need advanced features?**
→ [JSON Matching](05-json-matching.md) - JSONPath support

---

**[← Quick Start](01-quick-start.md)** | **[Back to Index](00-index.md)** | **[Next: Headers & Params →](03-headers-and-params.md)**
```

**Step 2: Verify content depth**

Run: `wc -w request-utils/docs/tutorial/02-core-concepts.md`
Expected: Approximately 1200-1500 words

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/02-core-concepts.md
git commit -m "docs(tutorial): add core concepts with architecture details"
```

---

## Task 5: Headers and Query Parameters (03-headers-and-params.md)

**Files:**
- Create: `request-utils/docs/tutorial/03-headers-and-params.md`

**Step 1: Write headers and params content**

Due to length constraints, I'll create the file with comprehensive content covering all scenarios mentioned in the design.

Create: `request-utils/docs/tutorial/03-headers-and-params.md`

Content includes:
- Common matching scenarios organized by use case
- Header and QueryParam side-by-side comparisons
- Real-world examples: API keys, content negotiation, feature flags, versioning
- Troubleshooting section
- Code examples for each scenario
- Approximately 1000-1200 words

**Step 2: Verify structure**

Run: `grep "^##" request-utils/docs/tutorial/03-headers-and-params.md`
Expected: Multiple section headers visible

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/03-headers-and-params.md
git commit -m "docs(tutorial): add headers and params practical guide"
```

---

## Task 6: Building Complex Conditions (04-building-complex-conditions.md)

**Files:**
- Create: `request-utils/docs/tutorial/04-building-complex-conditions.md`

**Step 1: Write complex conditions content**

Create file with content covering:
- AND logic with examples
- OR logic with examples
- Nested groups
- Builder API usage
- Evaluation mode selection
- Anti-patterns
- Performance tips
- Approximately 900-1000 words

**Step 2: Verify content**

Run: `grep -c "```java" request-utils/docs/tutorial/04-building-complex-conditions.md`
Expected: Multiple code examples (10+)

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/04-building-complex-conditions.md
git commit -m "docs(tutorial): add complex conditions with AND/OR logic"
```

---

## Task 7: JSON Matching (05-json-matching.md)

**Files:**
- Create: `request-utils/docs/tutorial/05-json-matching.md`

**Step 1: Write JSON matching content**

Create file with content covering:
- Prerequisites and dependency setup
- JSONPath basics
- JsonPathCondition usage
- JsonExactMatchCondition usage
- Combining with other conditions
- Troubleshooting JSON issues
- Multi-tenant example
- Approximately 800-900 words

**Step 2: Verify JSONPath examples**

Run: `grep "JSONPath" request-utils/docs/tutorial/05-json-matching.md | wc -l`
Expected: Multiple references to JSONPath

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/05-json-matching.md
git commit -m "docs(tutorial): add JSON matching with JSONPath guide"
```

---

## Task 8: AOP Annotations (06-aop-annotations.md)

**Files:**
- Create: `request-utils/docs/tutorial/06-aop-annotations.md`

**Step 1: Write AOP annotations content**

Create file with content covering:
- Programmatic vs declarative comparison
- @JUFUMatchConditions usage
- Inline conditions
- Exception handling
- Request extraction
- When NOT to use AOP
- Complete endpoint example
- Approximately 900-1000 words

**Step 2: Verify annotation examples**

Run: `grep "@JUFU" request-utils/docs/tutorial/06-aop-annotations.md | wc -l`
Expected: Multiple annotation references

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/06-aop-annotations.md
git commit -m "docs(tutorial): add AOP annotations declarative guide"
```

---

## Task 9: Custom Conditions (07-custom-conditions.md)

**Files:**
- Create: `request-utils/docs/tutorial/07-custom-conditions.md`

**Step 1: Write custom conditions content**

Create file with content covering:
- When to write custom conditions
- Condition interface deep dive
- Working hours example (complete implementation)
- IP whitelist example
- Rate limit example
- Testing patterns
- Performance considerations
- Approximately 1000-1200 words

**Step 2: Verify complete implementations**

Run: `grep "public class.*Condition implements Condition" request-utils/docs/tutorial/07-custom-conditions.md | wc -l`
Expected: Multiple complete condition implementations

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/07-custom-conditions.md
git commit -m "docs(tutorial): add custom conditions implementation guide"
```

---

## Task 10: Complete Examples (08-complete-examples.md)

**Files:**
- Create: `request-utils/docs/tutorial/08-complete-examples.md`

**Step 1: Write complete examples content**

Create file with content covering:
- API Versioning System (complete controller)
- Multi-Tenant SaaS Router (service + controller)
- Feature Flag System (AOP-based)
- Smart API Gateway (all features)
Each with:
  - Full file structure
  - Complete runnable code
  - Test cases
  - Design decisions
- Approximately 1500-2000 words

**Step 2: Verify completeness**

Run: `grep "package com\\." request-utils/docs/tutorial/08-complete-examples.md | wc -l`
Expected: Multiple package declarations (complete code)

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/08-complete-examples.md
git commit -m "docs(tutorial): add complete working examples"
```

---

## Task 11: Troubleshooting (09-troubleshooting.md)

**Files:**
- Create: `request-utils/docs/tutorial/09-troubleshooting.md`

**Step 1: Write troubleshooting content**

Create file with content organized by symptoms:
- "My condition isn't matching" - diagnostics and solutions
- "NullPointerException or missing request" - AOP issues
- "JSON matching not working" - JSONPath problems
- "AOP aspect not triggering" - configuration issues
- "Performance issues" - optimization guidance
- "ConditionNotMetException handling" - exception patterns
- FAQ section
- Approximately 1200-1400 words

**Step 2: Verify problem-solution structure**

Run: `grep "^###" request-utils/docs/tutorial/09-troubleshooting.md | wc -l`
Expected: Multiple symptom sections

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/09-troubleshooting.md
git commit -m "docs(tutorial): add comprehensive troubleshooting guide"
```

---

## Task 12: Tutorial README

**Files:**
- Create: `request-utils/docs/tutorial/README.md`

**Step 1: Create tutorial README**

Create: `request-utils/docs/tutorial/README.md`

```markdown
# Request Utils Tutorial

This directory contains comprehensive tutorial documentation for the request-utils module.

## Getting Started

**Start here:** [00-index.md](00-index.md)

The index page provides:
- Tutorial overview
- Multiple learning paths
- Navigation table
- Prerequisites

## Tutorial Structure

All tutorial files are numbered for sequential reading:

- **00-index.md** - Start here, navigation and learning paths
- **01-quick-start.md** - 10-minute getting started guide
- **02-core-concepts.md** - Architecture and fundamentals
- **03-headers-and-params.md** - Practical matching scenarios
- **04-building-complex-conditions.md** - AND/OR logic and nesting
- **05-json-matching.md** - JSONPath and exact field matching
- **06-aop-annotations.md** - Declarative annotation usage
- **07-custom-conditions.md** - Building custom logic
- **08-complete-examples.md** - Real-world applications
- **09-troubleshooting.md** - Debug and optimize

## Learning Paths

Choose your path:

**🚀 Fast Track (30 min):** 01 → 08
**📚 Comprehensive (90 min):** 01 → 02 → 03 → 04 → 08
**🎓 Full Course (3-4 hours):** 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09
**🔧 Problem-Driven:** Start at 09, jump to relevant sections

## Examples

The `examples/` directory contains runnable code samples referenced in the tutorial.

## Additional Resources

- [Main README](../../README.md) - API reference
- [GitHub Repository](https://github.com/abudhahir/java-utility-functions)

---

**Ready?** [Start with the Index →](00-index.md)
```

**Step 2: Verify README**

Run: `cat request-utils/docs/tutorial/README.md | head -10`
Expected: README with clear structure

**Step 3: Commit**

```bash
git add request-utils/docs/tutorial/README.md
git commit -m "docs(tutorial): add tutorial README with navigation"
```

---

## Task 13: Link Tutorial from Main README

**Files:**
- Modify: `request-utils/README.md:27-28`

**Step 1: Read current README to locate insertion point**

Run: `grep -n "## Installation" request-utils/README.md`
Expected: Line number where Installation section starts

**Step 2: Add tutorial link to README**

Add after the "Table of Contents" section (around line 27), before "## Overview":

```markdown
## 📚 New to Request Utils?

**[Start with the Tutorial →](docs/tutorial/00-index.md)** - Comprehensive getting started guide with examples

---
```

**Step 3: Verify link works**

Run: `grep -A2 "Start with the Tutorial" request-utils/README.md`
Expected: Tutorial link visible

**Step 4: Commit**

```bash
git add request-utils/README.md
git commit -m "docs: add tutorial link to main README"
```

---

## Task 14: Final Review and Testing

**Files:**
- Test: All tutorial markdown files
- Verify: All links work

**Step 1: Verify all files created**

Run: `ls -1 request-utils/docs/tutorial/*.md | sort`
Expected: All 11 files listed (00-index through 09-troubleshooting, plus README)

**Step 2: Check for broken internal links**

Run: `grep -r "\](.*\.md)" request-utils/docs/tutorial/*.md | grep -v "http"`
Expected: All links use relative paths

**Step 3: Verify word counts meet targets**

Run: `for f in request-utils/docs/tutorial/0*.md; do echo "$f: $(wc -w < $f) words"; done`
Expected: Files within target ranges specified in design

**Step 4: Check code block formatting**

Run: `grep -c '```java' request-utils/docs/tutorial/*.md`
Expected: Many Java code blocks across all files

**Step 5: Final commit**

```bash
git add request-utils/docs/tutorial/
git commit -m "docs(tutorial): finalize all tutorial documentation"
```

---

## Summary

This implementation plan creates comprehensive tutorial documentation for request-utils:

**Deliverables:**
- 10 tutorial markdown files (00-09)
- 1 tutorial README
- Updated main README with tutorial link
- Examples directory structure

**Content Coverage:**
- Quick start (immediate productivity)
- Core concepts (deep understanding)
- Practical guides (headers, params, complex conditions)
- Advanced topics (JSON, AOP, custom conditions)
- Complete examples (real-world applications)
- Troubleshooting (problem solving)

**Learning Paths:**
- Fast track: 30 minutes
- Comprehensive: 90 minutes
- Full course: 3-4 hours
- Problem-driven: Variable

**Total Estimated Time:** 3-4 hours to implement all documentation

**Files Modified:** 1 (README.md)
**Files Created:** 12 (tutorial files + README)
**Commits:** 14 (one per task)

---

## Execution Notes

**Prerequisites:**
- Working in `.worktrees/tutorial-docs` worktree
- All existing tests passing (verified: 100 tests, 0 failures)
- Maven build successful

**File Length Targets:**
- 01-quick-start.md: ~300 words
- 02-core-concepts.md: ~800 words
- 03-headers-and-params.md: ~1000 words
- 04-building-complex-conditions.md: ~900 words
- 05-json-matching.md: ~800 words
- 06-aop-annotations.md: ~900 words
- 07-custom-conditions.md: ~1000 words
- 08-complete-examples.md: ~1500 words
- 09-troubleshooting.md: ~1200 words

**Quality Checks:**
- All code examples include imports
- All examples are Spring Boot 4.0.3+ compatible
- All links use relative paths
- All files have navigation footer
- Consistent formatting throughout
