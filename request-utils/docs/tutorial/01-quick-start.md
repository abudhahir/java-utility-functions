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
