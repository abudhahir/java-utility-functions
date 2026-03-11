# Spring Modulith Integration Plan for request-utils

**Date:** March 9, 2026  
**Project:** request-utils  
**Purpose:** Refactor request-utils to use Spring Modulith for better modularity

---

## Current State Analysis

### Current Package Structure

```
com.cleveloper.jufu.requestutils/
├── RequestUtilsApplication.java
└── condition/
    ├── annotations/          # AOP annotations
    ├── aop/                  # AOP aspect implementation
    ├── builder/              # Fluent builders
    ├── config/               # Auto-configuration
    ├── core/                 # Core interfaces and classes
    └── matchers/             # Condition implementations
```

### Issues with Current Structure

1. **No clear module boundaries** - Everything is in one package hierarchy
2. **Mixed concerns** - AOP, core logic, and builders are intermingled
3. **No encapsulation** - Internal implementation classes are publicly accessible
4. **Difficult to test independently** - Modules are tightly coupled
5. **Hard to extend** - Adding new features requires touching multiple areas

---

## Proposed Modular Structure

### New Package Organization

```
com.cleveloper.jufu.requestutils/
├── RequestUtilsApplication.java
│
├── core/                              # Module 1: Core Condition API
│   ├── Condition.java                 # Public interface
│   ├── ConditionResult.java           # Public interface
│   ├── ConditionFailure.java          # Public interface
│   ├── ConditionGroup.java            # Public interface
│   ├── RequestContext.java            # Public interface
│   ├── EvaluationMode.java            # Public enum
│   ├── GroupOperator.java             # Public enum
│   ├── MatchOperation.java            # Public enum
│   └── internal/
│       ├── RequestContextImpl.java
│       ├── ConditionGroupImpl.java
│       └── ConditionEvaluator.java
│
├── matcher/                           # Module 2: Condition Matchers
│   ├── RequestConditionMatcher.java   # Public service
│   ├── ConditionFactory.java          # Public factory
│   └── internal/
│       ├── HeaderCondition.java
│       ├── QueryParamCondition.java
│       ├── JsonPathCondition.java
│       ├── JsonExactMatchCondition.java
│       └── StringMatcher.java
│
├── builder/                           # Module 3: Fluent Builders
│   ├── ConditionGroupBuilder.java     # Public builder
│   └── internal/
│       └── BuilderImpl.java
│
├── aop/                               # Module 4: AOP Integration
│   ├── JUFUMatchConditions.java       # Public annotation
│   ├── JUFUCondition.java             # Public annotation
│   ├── JUFUHeader.java                # Public annotation
│   ├── JUFUQueryParam.java            # Public annotation
│   ├── JUFUJsonPath.java              # Public annotation
│   ├── JUFUJsonExactMatch.java        # Public annotation
│   ├── ConditionNotMetException.java  # Public exception
│   └── internal/
│       ├── ConditionMatchingAspect.java
│       └── AnnotationConditionParser.java
│
└── config/                            # Module 5: Configuration
    ├── ConditionMatcherAutoConfiguration.java
    └── internal/
        └── ConfigProperties.java
```

---

## Module Descriptions

### Module 1: core

**Purpose:** Core condition evaluation interfaces and contracts

**Public API:**
- `Condition` - Functional interface for conditions
- `ConditionResult` - Evaluation result
- `ConditionFailure` - Failure details
- `ConditionGroup` - Logical grouping of conditions
- `RequestContext` - Request abstraction
- Enums: `EvaluationMode`, `GroupOperator`, `MatchOperation`

**Internal:**
- Implementation classes
- Evaluation engine

**Dependencies:** None (pure core)

**Events Published:**
- `ConditionEvaluatedEvent` - When any condition is evaluated

### Module 2: matcher

**Purpose:** Concrete condition implementations and matching service

**Public API:**
- `RequestConditionMatcher` - Main service for evaluating conditions
- `ConditionFactory` - Factory for creating conditions

**Internal:**
- `HeaderCondition`
- `QueryParamCondition`
- `JsonPathCondition`
- `JsonExactMatchCondition`
- `StringMatcher`

**Dependencies:**
- `core` module (for interfaces)

**Events Published:**
- `MatchOperationEvent` - When match operations are performed

### Module 3: builder

**Purpose:** Fluent API for building complex conditions

**Public API:**
- `ConditionGroupBuilder` - Fluent builder interface

**Internal:**
- Builder implementation details

**Dependencies:**
- `core` module
- `matcher` module (for creating conditions)

### Module 4: aop

**Purpose:** Annotation-based declarative condition matching

**Public API:**
- All AOP annotations
- `ConditionNotMetException`

**Internal:**
- `ConditionMatchingAspect` - AspectJ implementation
- `AnnotationConditionParser` - Annotation processing

**Dependencies:**
- `core` module
- `matcher` module

**Events Published:**
- `ConditionNotMetEvent` - When conditions fail in AOP

**Events Consumed:**
- None (terminal module)

### Module 5: config

**Purpose:** Spring Boot auto-configuration

**Public API:**
- `ConditionMatcherAutoConfiguration`

**Internal:**
- Configuration properties

**Dependencies:**
- All modules (wires everything together)

---

## Implementation Plan

### Phase 1: Preparation (Day 1)

#### Task 1.1: Add Spring Modulith Dependencies

Add to `pom.xml`:

```xml
<properties>
    <spring-modulith.version>1.2.2</spring-modulith.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>${spring-modulith.version}</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Existing dependencies -->
    
    <!-- Spring Modulith -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-observability</artifactId>
    </dependency>
</dependencies>
```

#### Task 1.2: Create Module Structure

Create new package structure (empty directories first):

```
condition/
├── core/internal/
├── matcher/internal/
├── builder/internal/
├── aop/internal/
└── config/internal/
```

### Phase 2: Refactor Core Module (Day 2)

#### Task 2.1: Move Core Interfaces

Move these to `core/`:
- `Condition.java`
- `ConditionResult.java`
- `ConditionFailure.java`
- `ConditionGroup.java`
- `RequestContext.java`
- `EvaluationMode.java`
- `GroupOperator.java`
- `MatchOperation.java`

#### Task 2.2: Move Core Implementations to Internal

Move these to `core/internal/`:
- `RequestContextImpl.java`

Update package declarations and imports.

#### Task 2.3: Create Event Classes

Create `core/events/`:
```java
package com.cleveloper.jufu.requestutils.core.events;

public record ConditionEvaluatedEvent(
    String conditionType,
    boolean matched,
    long evaluationTimeMs
) {}
```

### Phase 3: Refactor Matcher Module (Day 3)

#### Task 3.1: Move Matcher Classes

Move to `matcher/internal/`:
- All classes from `matchers/` package

#### Task 3.2: Create Public Matcher Service

Create `matcher/RequestConditionMatcher.java`:
```java
package com.cleveloper.jufu.requestutils.matcher;

@Service
public class RequestConditionMatcher {
    private final ApplicationEventPublisher events;
    
    public ConditionResult evaluate(Condition condition, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        
        RequestContext context = new RequestContextImpl(request);
        ConditionResult result = condition.evaluate(context);
        
        // Publish event
        events.publishEvent(new ConditionEvaluatedEvent(
            condition.getClass().getSimpleName(),
            result.isMatched(),
            System.currentTimeMillis() - startTime
        ));
        
        return result;
    }
}
```

#### Task 3.3: Create Factory

Create `matcher/ConditionFactory.java`:
```java
package com.cleveloper.jufu.requestutils.matcher;

@Component
public class ConditionFactory {
    public Condition createHeaderCondition(String name, String value, MatchOperation op, boolean ignoreCase) {
        return new HeaderCondition(name, value, op, ignoreCase);
    }
    
    public Condition createQueryParamCondition(String name, String value, MatchOperation op, boolean ignoreCase) {
        return new QueryParamCondition(name, value, op, ignoreCase);
    }
    
    // ... other factory methods
}
```

### Phase 4: Refactor Builder Module (Day 4)

#### Task 4.1: Move Builder Classes

Move to `builder/`:
- `ConditionGroupBuilder.java`

#### Task 4.2: Update Builder Dependencies

Ensure builder uses public APIs only:
- Use `ConditionFactory` instead of directly creating conditions
- Use `core` interfaces

### Phase 5: Refactor AOP Module (Day 5)

#### Task 5.1: Move AOP Classes

Move annotations to `aop/`:
- All annotation classes

Move implementations to `aop/internal/`:
- `ConditionMatchingAspect.java`
- `AnnotationConditionParser.java`

#### Task 5.2: Add Event Publishing

Update `ConditionMatchingAspect`:
```java
@Aspect
@Component
class ConditionMatchingAspect {
    private final ApplicationEventPublisher events;
    
    @Around("@annotation(matchConditions)")
    public Object checkConditions(ProceedingJoinPoint joinPoint, JUFUMatchConditions matchConditions) {
        // Existing logic...
        
        if (!result.isMatched()) {
            // Publish event before throwing exception
            events.publishEvent(new ConditionNotMetEvent(
                joinPoint.getSignature().getName(),
                result.getFailures()
            ));
            
            throw new ConditionNotMetException(result);
        }
        
        return joinPoint.proceed();
    }
}
```

### Phase 6: Refactor Config Module (Day 6)

#### Task 6.1: Move Configuration

Move to `config/`:
- `ConditionMatcherAutoConfiguration.java`

#### Task 6.2: Add Module Configuration

Update auto-configuration to wire modules together.

### Phase 7: Testing & Verification (Day 7)

#### Task 7.1: Create Module Verification Tests

Create `src/test/java/.../ModularityTests.java`:

```java
@SpringBootTest
class ModularityTests {

    @Test
    void shouldVerifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(RequestUtilsApplication.class);
        
        // This will fail if any module violates encapsulation
        modules.verify();
    }
    
    @Test
    void shouldPrintModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(RequestUtilsApplication.class);
        
        System.out.println("=== Request Utils Modules ===");
        modules.forEach(module -> {
            System.out.println("\nModule: " + module.getName());
            System.out.println("Base Package: " + module.getBasePackage());
            System.out.println("Spring Beans:");
            module.getSpringBeans().forEach(bean -> 
                System.out.println("  - " + bean.getFullyQualifiedTypeName())
            );
            System.out.println("Dependencies:");
            module.getDependencies().forEach(dep ->
                System.out.println("  - " + dep.getName())
            );
        });
    }
    
    @Test
    void shouldGenerateModuleDocumentation() {
        ApplicationModules modules = ApplicationModules.of(RequestUtilsApplication.class);
        
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
        
        // Documentation will be in target/spring-modulith-docs/
    }
}
```

#### Task 7.2: Run Existing Tests

Ensure all existing tests still pass:
```bash
mvn clean test
```

#### Task 7.3: Update Integration Tests

Update integration tests to use modular structure.

### Phase 8: Documentation (Day 8)

#### Task 8.1: Update README

Add Spring Modulith section to README:

```markdown
## Architecture

Request Utils uses Spring Modulith for a clean modular architecture:

### Modules

- **core** - Core condition interfaces and contracts
- **matcher** - Condition matchers and evaluation service
- **builder** - Fluent API for building conditions
- **aop** - Annotation-based declarative matching
- **config** - Spring Boot auto-configuration

### Module Diagram

![Module Diagram](docs/module-diagram.png)

See [Architecture Documentation](docs/architecture.md) for details.
```

#### Task 8.2: Create Architecture Document

Create `docs/architecture.md` explaining the modular design.

#### Task 8.3: Generate Module Diagrams

Run tests to generate PlantUML diagrams, convert to PNG, and add to docs.

---

## Migration Checklist

- [ ] Add Spring Modulith dependencies
- [ ] Create new package structure
- [ ] Move core interfaces to `core/`
- [ ] Move core implementations to `core/internal/`
- [ ] Move matchers to `matcher/internal/`
- [ ] Create public `RequestConditionMatcher` service
- [ ] Move builder to `builder/`
- [ ] Move AOP to `aop/` and `aop/internal/`
- [ ] Move config to `config/`
- [ ] Add event publishing
- [ ] Create verification tests
- [ ] Run all existing tests
- [ ] Generate module documentation
- [ ] Update README
- [ ] Create architecture documentation

---

## Expected Benefits

### 1. Better Encapsulation

**Before:**
```java
// Any class can access internal implementation
HeaderCondition condition = new HeaderCondition(...);
```

**After:**
```java
// Must use factory or builder
Condition condition = conditionFactory.createHeaderCondition(...);
// OR
Condition condition = ConditionGroup.builder()
    .header("X-Api-Key", "value")
    .build();
```

### 2. Independent Testing

**Before:**
```java
@Test
void testAOPWithMatcherLogic() {
    // Test couples AOP and matcher logic
}
```

**After:**
```java
@Test
void testCoreModuleAlone() {
    // Test core evaluation independently
}

@Test
void testMatcherModuleAlone() {
    // Test matchers independently
}

@Test
void testAOPModuleAlone() {
    // Test AOP independently
}
```

### 3. Clear Dependencies

Module dependency graph:
```
config
  ↓
aop → matcher → core
  ↓       ↓
builder ──┘
```

### 4. Event-Driven Architecture

```java
// Monitoring module (future addition)
@Service
public class ConditionMonitoringService {
    
    @ApplicationModuleListener
    public void onConditionEvaluated(ConditionEvaluatedEvent event) {
        // Track performance metrics
        metrics.recordEvaluation(
            event.conditionType(),
            event.evaluationTimeMs()
        );
    }
    
    @ApplicationModuleListener
    public void onConditionNotMet(ConditionNotMetEvent event) {
        // Log failures for debugging
        logger.warn("Condition failed: {}", event.failures());
    }
}
```

### 5. Better Documentation

Auto-generated module diagrams show:
- Module boundaries
- Dependencies between modules
- Public APIs
- Event flows

---

## Future Enhancements

With modular structure in place, we can easily add:

### 1. Caching Module

```
caching/
├── CachingService.java
└── internal/
    └── ConditionCache.java
```

Listen to `ConditionEvaluatedEvent` and cache results.

### 2. Monitoring Module

```
monitoring/
├── MonitoringService.java
└── internal/
    ├── MetricsCollector.java
    └── PerformanceTracker.java
```

Track all condition evaluations.

### 3. Validation Module

```
validation/
├── ValidationService.java
└── internal/
    └── ConditionValidator.java
```

Validate condition configurations.

---

## Testing Strategy

### Unit Tests

Each module has its own unit tests:

```
test/java/.../core/
├── ConditionTest.java
├── ConditionResultTest.java
└── ...

test/java/.../matcher/internal/
├── HeaderConditionTest.java
├── QueryParamConditionTest.java
└── ...

test/java/.../aop/internal/
├── ConditionMatchingAspectTest.java
└── ...
```

### Integration Tests

Test module interactions:

```java
@SpringBootTest
class ModuleIntegrationTests {

    @Test
    void aopShouldUseMatcherModule(Scenario scenario) {
        // Call AOP-annotated method
        controller.annotatedMethod();
        
        // Verify matcher was called via events
        scenario.andWaitForStateChange(() -> matcherCalled)
            .andVerify(called -> assertThat(called).isTrue());
    }
}
```

### Module Verification Tests

```java
@Test
void shouldRespectModuleBoundaries() {
    ApplicationModules.of(RequestUtilsApplication.class).verify();
}
```

---

## Rollback Plan

If issues arise:

1. **Keep git branches:**
   - `main` - Current state
   - `feature/spring-modulith` - New modular structure

2. **Incremental migration:**
   - Migrate one module at a time
   - Keep tests passing at each step
   - Can rollback individual modules

3. **Feature flags:**
   ```java
   @ConditionalOnProperty(name = "jufu.modulith.enabled", havingValue = "true")
   ```

---

## Success Criteria

- [ ] All existing tests pass
- [ ] Module verification tests pass
- [ ] No direct dependencies on `internal/` packages
- [ ] Module documentation generated
- [ ] README updated
- [ ] No regression in functionality
- [ ] Performance is same or better
- [ ] Code coverage maintained or improved

---

## Timeline

| Day | Phase | Tasks | Hours |
|-----|-------|-------|-------|
| 1 | Preparation | Dependencies, structure | 4h |
| 2 | Core | Move interfaces, implementations | 6h |
| 3 | Matcher | Refactor matchers, create service | 6h |
| 4 | Builder | Refactor builders | 4h |
| 5 | AOP | Refactor AOP, add events | 6h |
| 6 | Config | Update configuration | 4h |
| 7 | Testing | Verification, all tests | 8h |
| 8 | Documentation | README, architecture docs | 4h |

**Total:** ~42 hours (~1 week full-time or 2 weeks part-time)

---

## Questions & Decisions

### Q1: Should we make HeaderCondition public?

**Decision:** No, keep it internal. Use factory or builder to create conditions.

**Rationale:** Better encapsulation, easier to change implementation later.

### Q2: Should we use events for all module communication?

**Decision:** Use events for notifications, direct dependencies for synchronous operations.

**Rationale:** Balance between loose coupling and practical synchronous needs.

### Q3: Should we add monitoring module now?

**Decision:** No, add in Phase 2 after modular structure is stable.

**Rationale:** Focus on core refactoring first, add features later.

---

## Conclusion

Spring Modulith integration will give request-utils:

✅ Clear module boundaries  
✅ Better encapsulation  
✅ Independent testing  
✅ Auto-generated documentation  
✅ Event-driven architecture  
✅ Easy extensibility  

The refactoring will improve maintainability while keeping all existing functionality intact.

---

**Created:** March 9, 2026  
**Status:** Planning  
**Next Step:** Review and approve plan

