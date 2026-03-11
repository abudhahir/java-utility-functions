# Spring Modulith Guide

**Last Updated:** March 9, 2026

## Table of Contents

- [What is Spring Modulith?](#what-is-spring-modulith)
- [Why Use Spring Modulith?](#why-use-spring-modulith)
- [Core Concepts](#core-concepts)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
- [Implementation Guide](#implementation-guide)
- [How to Use in Your request-utils Project](#how-to-use-in-your-request-utils-project)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)
- [References](#references)

---

## What is Spring Modulith?

**Spring Modulith is an opinionated toolkit for building domain-driven, modular Spring Boot applications.**

Instead of splitting your application into microservices, Spring Modulith helps you build a **modular monolith** - a single deployable application that is internally organized into well-defined, loosely-coupled modules based on your business domains.

### The Modular Monolith Approach

```
Traditional Monolith          Microservices              Modular Monolith (Spring Modulith)
┌──────────────────┐         ┌────────┐  ┌────────┐    ┌──────────────────────────────────┐
│                  │         │Service1│  │Service2│    │    Single Deployable Unit        │
│  All code mixed  │   VS    │        │  │        │    │  ┌────────┐  ┌────────┐         │
│  together        │         │  DB1   │  │  DB2   │    │  │Module A│  │Module B│         │
│                  │         └────────┘  └────────┘    │  │        │  │        │         │
└──────────────────┘                                    │  └────────┘  └────────┘         │
                                                        │  ┌────────┐  ┌────────┐         │
                                                        │  │Module C│  │Module D│         │
                                                        │  │        │  │        │         │
                                                        │  └────────┘  └────────┘         │
                                                        └──────────────────────────────────┘
```

---

## Why Use Spring Modulith?

### Advantages

✅ **Simplicity**: Deploy as a single unit (no distributed system complexity)  
✅ **Modularity**: Clear boundaries between business domains  
✅ **Maintainability**: Easier to understand and modify  
✅ **Testability**: Test modules independently  
✅ **Documentation**: Auto-generate module diagrams  
✅ **Evolution Path**: Easy migration to microservices if needed  
✅ **Performance**: No network overhead between modules  
✅ **Development Speed**: Faster development and debugging  

### When to Use Spring Modulith?

- **Starting a new project** and not sure if you need microservices yet
- **Refactoring a monolith** to have better structure
- Building **medium-sized applications** with multiple domains
- Want **domain-driven design** without microservices complexity
- Need to **maintain team boundaries** within a monolith
- Want to **avoid distributed system problems** (network failures, eventual consistency, etc.)

---

## Core Concepts

### 1. Application Module

**An application module is a unit of functionality representing a business domain.**

- Located as a **direct sub-package** of your main application package
- Has a **public API** (exposed interfaces/classes)
- Has **internal implementation** (hidden from other modules)
- Can **publish and consume events** to communicate with other modules

### 2. Module Structure

```
src/main/java/com/example/myapp/
├── MyApplication.java                    # Main Spring Boot class
├── order/                                # Order Module (Public API)
│   ├── Order.java
│   ├── OrderService.java                 # Public API
│   └── internal/                         # Internal implementation
│       ├── OrderRepository.java
│       ├── OrderEntity.java
│       └── OrderValidator.java
├── payment/                              # Payment Module
│   ├── Payment.java
│   ├── PaymentService.java
│   └── internal/
│       ├── PaymentGateway.java
│       └── PaymentRepository.java
└── notification/                         # Notification Module
    ├── NotificationService.java
    └── internal/
        ├── EmailSender.java
        └── SmsSender.java
```

**Key Rules:**
- Code in `order/` can be accessed by other modules
- Code in `order/internal/` **cannot** be accessed by other modules
- Modules communicate via **public APIs** or **events**

### 3. Module Encapsulation

Spring Modulith enforces module boundaries:

```java
// ✅ ALLOWED: Accessing public API
// In payment module
@Service
public class PaymentService {
    private final OrderService orderService; // Public API
    
    public void processPayment(String orderId) {
        Order order = orderService.getOrder(orderId);
        // ...
    }
}

// ❌ NOT ALLOWED: Accessing internal implementation
// In payment module
@Service
public class PaymentService {
    private final OrderRepository orderRepository; // Internal class
    
    public void processPayment(String orderId) {
        OrderEntity order = orderRepository.findById(orderId); // Violation!
        // ...
    }
}
```

### 4. Inter-Module Communication

**Two approaches:**

#### A. Direct Bean Dependency (Tight Coupling)

```java
@Service
public class OrderService {
    private final NotificationService notificationService;
    
    public void createOrder(Order order) {
        // ... create order
        notificationService.sendConfirmation(order); // Direct call
    }
}
```

#### B. Event-Based Communication (Loose Coupling) ⭐ Recommended

```java
// Order Module - Publisher
@Service
public class OrderService {
    private final ApplicationEventPublisher events;
    
    public void createOrder(Order order) {
        // ... create order
        events.publishEvent(new OrderCreatedEvent(order)); // Publish event
    }
}

// Notification Module - Subscriber
@Service
public class NotificationService {
    
    @ApplicationModuleListener  // Spring Modulith annotation
    public void onOrderCreated(OrderCreatedEvent event) {
        sendConfirmation(event.getOrder()); // Handle event
    }
}
```

---

## Key Features

### 1. Module Verification

Spring Modulith can verify that your code respects module boundaries:

```java
@Test
void shouldRespectModuleBoundaries() {
    ApplicationModules modules = ApplicationModules.of(Application.class);
    modules.verify(); // Fails if modules violate encapsulation rules
}
```

**Example violation:**
```
org.springframework.modulith.core.Violations:
- Module 'payment' depends on non-exposed type 
  com.example.order.internal.OrderRepository within module 'order'!
```

### 2. Module Documentation

Auto-generate documentation and diagrams:

```java
@Test
void generateModuleDocumentation() {
    ApplicationModules modules = ApplicationModules.of(Application.class);
    new Documenter(modules)
        .writeDocumentation()
        .writeIndividualModulesAsPlantUml();
}
```

Generates:
- **PlantUML diagrams** showing module dependencies
- **C4 component diagrams**
- **Module relationship documentation**

### 3. Event Publication Registry

Spring Modulith can track published events for reliability:

```java
@ApplicationModuleListener
@Transactional
public void onOrderCreated(OrderCreatedEvent event) {
    // If this fails, event will be retried
    sendEmail(event.getOrder());
}
```

### 4. Observability

Integration with Spring Boot's observability features:
- **Application events tracking**
- **Module interaction monitoring**
- **Performance metrics per module**

---

## Getting Started

### Step 1: Add Dependencies

**Maven** (`pom.xml`):

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-bom</artifactId>
            <version>1.2.2</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Core API -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-api</artifactId>
    </dependency>
    
    <!-- Testing support -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Event externalization (optional) -->
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-jpa</artifactId>
    </dependency>
</dependencies>
```

**Gradle** (`build.gradle`):

```gradle
dependencyManagement {
    imports {
        mavenBom 'org.springframework.modulith:spring-modulith-bom:1.2.2'
    }
}

dependencies {
    implementation 'org.springframework.modulith:spring-modulith-api'
    testImplementation 'org.springframework.modulith:spring-modulith-starter-test'
    implementation 'org.springframework.modulith:spring-modulith-starter-jpa' // optional
}
```

### Step 2: Organize Your Code

Structure your packages as modules:

```
src/main/java/com/yourcompany/yourapp/
├── YourApplication.java          # @SpringBootApplication
├── module1/
│   ├── PublicApi.java
│   └── internal/
│       └── InternalClass.java
├── module2/
│   ├── PublicApi.java
│   └── internal/
│       └── InternalClass.java
└── shared/                       # Shared utilities (optional)
    └── CommonUtils.java
```

### Step 3: Write Verification Tests

```java
@SpringBootTest
class ModularityTests {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(Application.class).verify();
    }
    
    @Test
    void printsModules() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.forEach(System.out::println);
    }
}
```

### Step 4: Use Events for Communication

```java
// Event class
public record OrderCreatedEvent(String orderId, String customerId) {}

// Publisher
@Service
public class OrderService {
    private final ApplicationEventPublisher events;
    
    public void createOrder(OrderRequest request) {
        // ... save order
        events.publishEvent(new OrderCreatedEvent(order.getId(), order.getCustomerId()));
    }
}

// Subscriber
@Service
public class NotificationService {
    
    @ApplicationModuleListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Send notification
    }
}
```

---

## Implementation Guide

### Example: E-Commerce Application

Let's build a simple e-commerce app with three modules:

#### 1. Project Structure

```
src/main/java/com/example/ecommerce/
├── EcommerceApplication.java
├── product/
│   ├── Product.java
│   ├── ProductService.java
│   └── internal/
│       ├── ProductRepository.java
│       └── ProductEntity.java
├── order/
│   ├── Order.java
│   ├── OrderService.java
│   └── internal/
│       ├── OrderRepository.java
│       └── OrderEntity.java
└── notification/
    ├── NotificationService.java
    └── internal/
        ├── EmailService.java
        └── SmsService.java
```

#### 2. Product Module

```java
// product/Product.java (Public API)
public class Product {
    private String id;
    private String name;
    private BigDecimal price;
    // getters, setters, constructors
}

// product/ProductService.java (Public API)
@Service
public class ProductService {
    private final ProductRepository repository;
    
    public Product getProduct(String id) {
        return repository.findById(id)
            .map(this::toDto)
            .orElseThrow();
    }
    
    public List<Product> getAllProducts() {
        return repository.findAll().stream()
            .map(this::toDto)
            .toList();
    }
    
    private Product toDto(ProductEntity entity) {
        // mapping logic
    }
}

// product/internal/ProductRepository.java (Internal)
@Repository
interface ProductRepository extends JpaRepository<ProductEntity, String> {
}

// product/internal/ProductEntity.java (Internal)
@Entity
class ProductEntity {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    // ... entity fields
}
```

#### 3. Order Module

```java
// order/Order.java (Public API)
public class Order {
    private String id;
    private String customerId;
    private List<String> productIds;
    private BigDecimal totalAmount;
    // getters, setters
}

// order/events/OrderCreatedEvent.java (Public API)
public record OrderCreatedEvent(String orderId, String customerId, BigDecimal amount) {}

// order/OrderService.java (Public API)
@Service
public class OrderService {
    private final OrderRepository repository;
    private final ProductService productService; // Public API dependency
    private final ApplicationEventPublisher events;
    
    public Order createOrder(OrderRequest request) {
        // Validate products exist
        List<Product> products = request.getProductIds().stream()
            .map(productService::getProduct)
            .toList();
        
        // Calculate total
        BigDecimal total = products.stream()
            .map(Product::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Save order
        OrderEntity entity = new OrderEntity();
        entity.setCustomerId(request.getCustomerId());
        entity.setTotalAmount(total);
        OrderEntity saved = repository.save(entity);
        
        // Publish event
        events.publishEvent(new OrderCreatedEvent(
            saved.getId(),
            saved.getCustomerId(),
            saved.getTotalAmount()
        ));
        
        return toDto(saved);
    }
}

// order/internal/OrderRepository.java (Internal)
@Repository
interface OrderRepository extends JpaRepository<OrderEntity, String> {
}
```

#### 4. Notification Module

```java
// notification/NotificationService.java (Public API)
@Service
public class NotificationService {
    private final EmailService emailService;
    private final SmsService smsService;
    
    @ApplicationModuleListener // Event listener
    public void onOrderCreated(OrderCreatedEvent event) {
        emailService.sendOrderConfirmation(
            event.customerId(),
            event.orderId(),
            event.amount()
        );
        
        smsService.sendOrderNotification(
            event.customerId(),
            event.orderId()
        );
    }
}

// notification/internal/EmailService.java (Internal)
@Service
class EmailService {
    void sendOrderConfirmation(String customerId, String orderId, BigDecimal amount) {
        // Send email logic
    }
}

// notification/internal/SmsService.java (Internal)
@Service
class SmsService {
    void sendOrderNotification(String customerId, String orderId) {
        // Send SMS logic
    }
}
```

#### 5. Testing

```java
@SpringBootTest
class ModularityTests {

    @Test
    void shouldVerifyModularStructure() {
        ApplicationModules.of(EcommerceApplication.class).verify();
    }
    
    @Test
    void shouldPrintModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(EcommerceApplication.class);
        
        System.out.println("=== Application Modules ===");
        modules.forEach(module -> {
            System.out.println("\nModule: " + module.getName());
            System.out.println("Base Package: " + module.getBasePackage());
            System.out.println("Spring Beans: " + module.getSpringBeans().size());
        });
    }
    
    @Test
    void shouldGenerateDocumentation() {
        ApplicationModules modules = ApplicationModules.of(EcommerceApplication.class);
        
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
        
        // Check target/spring-modulith-docs for generated files
    }
}
```

---

## How to Use in Your request-utils Project

Your `request-utils` project is currently structured as a single module. Here's how you could apply Spring Modulith to make it more modular:

### Current Structure

```
request-utils/
└── src/main/java/com/cleveloper/jufu/requestutils/
    ├── RequestUtilsApplication.java
    └── condition/
        ├── annotations/
        ├── aop/
        ├── builder/
        ├── config/
        ├── core/
        └── matchers/
```

### Proposed Modular Structure with Spring Modulith

```
request-utils/
└── src/main/java/com/cleveloper/jufu/requestutils/
    ├── RequestUtilsApplication.java
    │
    ├── matcher/                          # Module 1: Core Matching
    │   ├── MatcherService.java           # Public API
    │   ├── MatchOperation.java           # Public API
    │   └── internal/
    │       ├── MatcherEngine.java
    │       └── PatternCache.java
    │
    ├── condition/                        # Module 2: Condition Engine
    │   ├── Condition.java                # Public API
    │   ├── ConditionResult.java          # Public API
    │   ├── ConditionGroup.java           # Public API
    │   └── internal/
    │       ├── ConditionEvaluator.java
    │       ├── RequestContextImpl.java
    │       └── matchers/
    │           ├── HeaderConditionImpl.java
    │           ├── QueryParamConditionImpl.java
    │           └── JsonConditionImpl.java
    │
    ├── aop/                              # Module 3: AOP Integration
    │   ├── JUFUMatchConditions.java      # Public API
    │   ├── JUFUHeader.java               # Public API
    │   ├── JUFUQueryParam.java           # Public API
    │   ├── ConditionNotMetException.java # Public API
    │   └── internal/
    │       ├── ConditionMatchingAspect.java
    │       └── AnnotationConditionParser.java
    │
    └── builder/                          # Module 4: Fluent Builders
        ├── ConditionBuilder.java         # Public API
        └── internal/
            └── ConditionGroupBuilderImpl.java
```

### Benefits for Your Project

1. **Clear Separation of Concerns**
   - Core matching logic isolated from AOP concerns
   - Builder API separated from condition implementation
   
2. **Better Testing**
   ```java
   @Test
   void shouldTestConditionModuleIndependently() {
       // Test condition logic without AOP aspects
       Condition condition = new HeaderCondition(...);
       ConditionResult result = condition.evaluate(context);
       // ...
   }
   ```

3. **Easier Maintenance**
   - Changes to AOP annotations don't affect core matching
   - Core condition logic can be modified independently

4. **Future Extensibility**
   - Easy to add new modules (e.g., `reporting`, `monitoring`, `caching`)
   - Each module can evolve independently

### Implementation Steps

#### 1. Add Spring Modulith Dependencies

```xml
<!-- Add to request-utils/pom.xml -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-api</artifactId>
    <version>1.2.2</version>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <version>1.2.2</version>
    <scope>test</scope>
</dependency>
```

#### 2. Reorganize Packages

Move internal implementations to `internal/` sub-packages:

```java
// BEFORE: condition/core/RequestContextImpl.java
package com.cleveloper.jufu.requestutils.condition.core;

// AFTER: condition/internal/RequestContextImpl.java
package com.cleveloper.jufu.requestutils.condition.internal;

// Public interface stays in parent package
// condition/RequestContext.java
package com.cleveloper.jufu.requestutils.condition;

public interface RequestContext {
    // Public API
}
```

#### 3. Use Events for Module Communication

If AOP module needs to notify other modules:

```java
// aop/events/ConditionEvaluatedEvent.java
public record ConditionEvaluatedEvent(
    String methodName,
    boolean matched,
    List<ConditionFailure> failures
) {}

// aop/internal/ConditionMatchingAspect.java
@Aspect
class ConditionMatchingAspect {
    private final ApplicationEventPublisher events;
    
    @Around("@annotation(matchConditions)")
    public Object checkConditions(...) {
        ConditionResult result = evaluateConditions(...);
        
        // Publish event
        events.publishEvent(new ConditionEvaluatedEvent(
            methodName,
            result.isMatched(),
            result.getFailures()
        ));
        
        // ...
    }
}

// monitoring/MonitoringService.java (new module!)
@Service
public class MonitoringService {
    
    @ApplicationModuleListener
    public void onConditionEvaluated(ConditionEvaluatedEvent event) {
        // Track metrics
        if (!event.matched()) {
            logConditionFailure(event);
        }
    }
}
```

#### 4. Add Verification Tests

```java
@SpringBootTest
class RequestUtilsModularityTests {

    @Test
    void shouldVerifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(RequestUtilsApplication.class);
        modules.verify();
    }
    
    @Test
    void shouldShowModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(RequestUtilsApplication.class);
        
        modules.forEach(module -> {
            System.out.println("Module: " + module.getName());
            System.out.println("  Beans: " + module.getSpringBeans());
            System.out.println("  Dependencies: " + module.getDependencies());
        });
    }
}
```

#### 5. Generate Documentation

```java
@Test
void shouldGenerateModuleDiagrams() {
    ApplicationModules modules = ApplicationModules.of(RequestUtilsApplication.class);
    
    new Documenter(modules)
        .writeDocumentation()
        .writeIndividualModulesAsPlantUml();
}
```

---

## Best Practices

### 1. Keep Modules Focused

Each module should represent a single business capability:

✅ **Good:**
```
order/          # Everything about orders
payment/        # Everything about payments
shipping/       # Everything about shipping
```

❌ **Bad:**
```
services/       # Too broad
utils/          # Not a business domain
data/           # Technical grouping
```

### 2. Use Events Over Direct Dependencies

**When to use events:**
- One module needs to react to another's actions
- Asynchronous processing is acceptable
- Loose coupling is desired

**When to use direct dependencies:**
- Synchronous response needed
- Strong consistency required
- Query operations (read-only)

### 3. Design Public APIs Carefully

```java
// ✅ Good: Clear, minimal public API
// order/Order.java
public class Order {
    private String id;
    private BigDecimal total;
    // Only expose what others need
}

// ❌ Bad: Exposing internal details
// order/Order.java
public class Order {
    private OrderEntity entity;  // Don't expose JPA entities!
    public OrderEntity getEntity() { return entity; }
}
```

### 4. Test Module Boundaries

```java
@Test
void shouldNotAllowAccessToInternals() {
    ApplicationModules modules = ApplicationModules.of(App.class);
    
    // This will fail if any module violates encapsulation
    modules.verify();
}
```

### 5. Document Module Dependencies

```java
// In module documentation
/**
 * Order Module
 * 
 * Dependencies:
 * - product (for product validation)
 * 
 * Events Published:
 * - OrderCreatedEvent
 * - OrderCancelledEvent
 * 
 * Events Consumed:
 * - PaymentCompletedEvent
 */
```

### 6. Start Simple, Refactor as Needed

Don't over-modularize from the start:
1. Start with 2-3 clear business modules
2. Add more modules as complexity grows
3. Refactor when module boundaries become clearer

---

## Real-World Examples

### Example 1: SaaS Application

```
src/main/java/com/example/saas/
├── SaasApplication.java
├── auth/                     # Authentication & Authorization
│   ├── UserService.java
│   ├── LoginController.java
│   └── internal/
├── tenant/                   # Multi-tenancy Management
│   ├── TenantService.java
│   └── internal/
├── subscription/             # Billing & Subscriptions
│   ├── SubscriptionService.java
│   └── internal/
├── notification/             # Email, SMS, Push
│   ├── NotificationService.java
│   └── internal/
└── analytics/                # Usage Tracking
    ├── AnalyticsService.java
    └── internal/
```

**Event Flow:**
```
User signs up → auth publishes UserRegisteredEvent
               → tenant listens and creates tenant
               → subscription listens and starts trial
               → notification listens and sends welcome email
```

### Example 2: E-Learning Platform

```
src/main/java/com/example/elearning/
├── ElearningApplication.java
├── course/                   # Course Management
│   ├── CourseService.java
│   └── internal/
├── enrollment/               # Student Enrollment
│   ├── EnrollmentService.java
│   └── internal/
├── video/                    # Video Streaming
│   ├── VideoService.java
│   └── internal/
├── assessment/               # Quizzes & Exams
│   ├── AssessmentService.java
│   └── internal/
├── certificate/              # Certificate Generation
│   ├── CertificateService.java
│   └── internal/
└── reporting/                # Analytics & Reports
    ├── ReportingService.java
    └── internal/
```

### Example 3: Healthcare System

```
src/main/java/com/example/healthcare/
├── HealthcareApplication.java
├── patient/                  # Patient Records
│   ├── PatientService.java
│   └── internal/
├── appointment/              # Appointment Scheduling
│   ├── AppointmentService.java
│   └── internal/
├── prescription/             # Prescription Management
│   ├── PrescriptionService.java
│   └── internal/
├── billing/                  # Medical Billing
│   ├── BillingService.java
│   └── internal/
└── notification/             # Patient Communication
    ├── NotificationService.java
    └── internal/
```

---

## Advanced Features

### 1. Event Externalization

Persist events for reliability:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
</dependency>
```

```java
// Events are now persisted and retried on failure
@ApplicationModuleListener
@Transactional
public void onOrderCreated(OrderCreatedEvent event) {
    // If this fails, event will be retried
    externalApi.notifyOrderCreated(event);
}
```

### 2. Async Event Processing

```java
// Enable async in your application
@EnableAsync
@SpringBootApplication
public class Application {
    // ...
}

// Events are processed asynchronously
@ApplicationModuleListener  // Already includes @Async
public void onOrderCreated(OrderCreatedEvent event) {
    // Runs in separate thread
    sendEmailAsync(event);
}
```

### 3. Moments - Integration Testing

Test module interactions:

```java
@SpringBootTest
class OrderModuleTests {

    @Test
    void orderCreationShouldTriggerNotification(Scenario scenario) {
        // When
        orderService.createOrder(orderRequest);
        
        // Then verify event was published
        scenario.andWaitForStateChange(() -> 
            notificationSent()
        ).andVerify(result -> {
            assertThat(result).isTrue();
        });
    }
}
```

### 4. Observability Integration

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-observability</artifactId>
</dependency>
```

Automatic metrics and tracing for module interactions.

---

## Migration Strategy

### From Traditional Monolith to Spring Modulith

#### Step 1: Identify Domains

Analyze your current code and identify business domains:

```
Current Structure:
src/main/java/com/example/
├── controller/     (mixed concerns)
├── service/        (mixed concerns)
├── repository/     (mixed concerns)
└── model/          (mixed concerns)

Identify Domains:
- User Management
- Product Catalog
- Order Processing
- Payment
- Notification
```

#### Step 2: Create Module Packages

```
New Structure:
src/main/java/com/example/
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   └── internal/
│       └── UserRepository.java
├── product/
│   └── ...
├── order/
│   └── ...
└── payment/
    └── ...
```

#### Step 3: Move Code Gradually

1. Start with one module
2. Move related classes
3. Make internal classes package-private
4. Run verification tests
5. Repeat for next module

#### Step 4: Replace Direct Calls with Events

```java
// Before: Direct call
orderService.create(order);
paymentService.process(order);  // Direct dependency

// After: Event-based
orderService.create(order);
// publishes OrderCreatedEvent
// PaymentService listens to event
```

#### Step 5: Add Verification

```java
@Test
void verifyModularStructure() {
    ApplicationModules.of(Application.class).verify();
}
```

---

## Comparison: Spring Modulith vs Alternatives

| Feature | Spring Modulith | Microservices | Traditional Monolith |
|---------|----------------|---------------|---------------------|
| Deployment | Single unit | Multiple services | Single unit |
| Module boundaries | Enforced | Physical | None |
| Inter-module communication | Events + APIs | Network (REST/gRPC) | Direct calls |
| Testing complexity | Low | High | Medium |
| Operational complexity | Low | High | Low |
| Scalability | Vertical | Horizontal | Vertical |
| Team boundaries | Supported | Natural | Difficult |
| Migration path | To microservices | N/A | Difficult |

---

## Troubleshooting

### Issue: Verification Fails

**Problem:**
```
org.springframework.modulith.core.Violations:
Module 'payment' depends on non-exposed type ...
```

**Solution:**
1. Check if you're accessing `internal/` packages from other modules
2. Move the class to public API or create a DTO
3. Use events instead of direct dependencies

### Issue: Events Not Received

**Problem:** Published event but listener not called

**Solution:**
1. Check `@ApplicationModuleListener` annotation
2. Verify `@EnableAsync` if using async processing
3. Check event type matches exactly
4. Ensure both modules are component-scanned

### Issue: Module Not Detected

**Problem:** Module doesn't appear in `ApplicationModules`

**Solution:**
1. Ensure module is a direct sub-package of main package
2. Check that package contains at least one Spring bean
3. Verify package naming conventions

---

## References

- **Official Documentation**: https://docs.spring.io/spring-modulith/reference/
- **GitHub Repository**: https://github.com/spring-projects/spring-modulith
- **Examples**: https://github.com/spring-projects/spring-modulith/tree/main/spring-modulith-examples
- **Baeldung Tutorial**: https://www.baeldung.com/spring-modulith
- **Spring Blog**: https://spring.io/blog/category/spring-modulith

---

## Conclusion

Spring Modulith provides a structured approach to building maintainable monolithic applications with clear module boundaries. It's ideal for:

- New projects where microservices might be overkill
- Refactoring existing monoliths
- Teams wanting domain-driven design without distributed system complexity
- Applications that may need to evolve to microservices later

**Key Takeaways:**

1. ✅ Organize code by business domain (modules)
2. ✅ Use `internal/` packages for encapsulation
3. ✅ Prefer events over direct dependencies
4. ✅ Verify module boundaries with tests
5. ✅ Generate documentation automatically
6. ✅ Keep public APIs minimal and clear

Start simple, verify often, and let your module structure evolve with your understanding of the domain!

---

**Last Updated:** March 9, 2026  
**Spring Modulith Version:** 1.2.2  
**Compatible with:** Spring Boot 3.x

