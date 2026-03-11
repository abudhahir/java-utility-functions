# Spring Modulith Quick Reference

**Last Updated:** March 9, 2026

---

## 📝 What is Spring Modulith?

**TL;DR:** Spring Modulith = Modular Monolith Architecture for Spring Boot

- ✅ Single deployable unit (like traditional monolith)
- ✅ Clear module boundaries (like microservices)
- ✅ Domain-driven design without distributed system complexity

---

## 🚀 Quick Start

### 1. Add Dependency

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
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Organize Packages

```
src/main/java/com/example/app/
├── Application.java              # Main class
├── order/                        # Module 1
│   ├── OrderService.java         # Public API
│   └── internal/                 # Hidden from other modules
│       └── OrderRepository.java
├── payment/                      # Module 2
│   ├── PaymentService.java
│   └── internal/
│       └── PaymentGateway.java
└── notification/                 # Module 3
    ├── NotificationService.java
    └── internal/
        └── EmailSender.java
```

### 3. Verify Structure

```java
@Test
void verifyModules() {
    ApplicationModules.of(Application.class).verify();
}
```

---

## 📐 Module Rules

### ✅ Allowed

```java
// ✅ Access public API from another module
@Service
public class PaymentService {
    private final OrderService orderService;  // Public API
    
    public void process(String orderId) {
        Order order = orderService.getOrder(orderId);
    }
}

// ✅ Use events for decoupled communication
@Service
public class OrderService {
    private final ApplicationEventPublisher events;
    
    public void createOrder(Order order) {
        events.publishEvent(new OrderCreatedEvent(order));
    }
}

// ✅ Listen to events from other modules
@Service
public class NotificationService {
    
    @ApplicationModuleListener
    public void onOrderCreated(OrderCreatedEvent event) {
        sendEmail(event.getOrder());
    }
}
```

### ❌ Not Allowed

```java
// ❌ Cannot access internal classes from other modules
@Service
public class PaymentService {
    private final OrderRepository orderRepository;  // Internal class!
    
    public void process(String orderId) {
        OrderEntity order = orderRepository.findById(orderId);  // Violation!
    }
}
```

---

## 🎯 Key Annotations

### `@ApplicationModuleListener`

Event listener that automatically includes:
- `@Async` - Asynchronous processing
- `@Transactional` - Transaction management
- `@TransactionalEventListener` - Event handling

```java
@ApplicationModuleListener
public void handleEvent(OrderCreatedEvent event) {
    // Runs async, in transaction, after commit
}
```

---

## 📊 Testing

### Verify Module Boundaries

```java
@Test
void shouldVerifyModularStructure() {
    ApplicationModules modules = ApplicationModules.of(Application.class);
    modules.verify();  // Fails if boundaries violated
}
```

### Print Module Structure

```java
@Test
void shouldPrintModules() {
    ApplicationModules modules = ApplicationModules.of(Application.class);
    modules.forEach(System.out::println);
}
```

### Generate Documentation

```java
@Test
void shouldGenerateDocs() {
    ApplicationModules modules = ApplicationModules.of(Application.class);
    new Documenter(modules)
        .writeDocumentation()
        .writeIndividualModulesAsPlantUml();
}
```

---

## 🎨 Common Patterns

### Pattern 1: Command-Query Separation

```java
// order/OrderService.java - Public API
@Service
public class OrderService {
    
    // Query (synchronous)
    public Order getOrder(String id) {
        return repository.findById(id).map(this::toDto).orElseThrow();
    }
    
    // Command (event-based)
    public void createOrder(OrderRequest request) {
        // Save order
        Order order = repository.save(toEntity(request));
        
        // Publish event
        events.publishEvent(new OrderCreatedEvent(order));
    }
}
```

### Pattern 2: Event-Driven Integration

```java
// Module A - Publisher
@Service
public class UserService {
    public void registerUser(UserRequest request) {
        User user = saveUser(request);
        events.publishEvent(new UserRegisteredEvent(user));
    }
}

// Module B - Subscriber
@Service
public class WelcomeService {
    @ApplicationModuleListener
    public void onUserRegistered(UserRegisteredEvent event) {
        sendWelcomeEmail(event.getUser());
    }
}

// Module C - Another Subscriber
@Service
public class AnalyticsService {
    @ApplicationModuleListener
    public void onUserRegistered(UserRegisteredEvent event) {
        trackRegistration(event.getUser());
    }
}
```

### Pattern 3: Internal Implementation

```java
// order/Order.java - Public DTO
public class Order {
    private String id;
    private BigDecimal total;
    // Only expose what's needed
}

// order/internal/OrderEntity.java - Internal JPA Entity
@Entity
class OrderEntity {
    @Id
    private String id;
    private BigDecimal total;
    private String internalStatus;  // Not exposed in DTO
    // ... internal fields
}

// order/internal/OrderRepository.java - Internal
@Repository
interface OrderRepository extends JpaRepository<OrderEntity, String> {
}
```

---

## 🔥 Common Mistakes

### ❌ Mistake 1: Exposing Entities

```java
// ❌ BAD: Exposing JPA entity
public OrderEntity getOrder(String id) {
    return repository.findById(id).orElseThrow();
}

// ✅ GOOD: Return DTO
public Order getOrder(String id) {
    return repository.findById(id)
        .map(this::toDto)
        .orElseThrow();
}
```

### ❌ Mistake 2: Not Using Internal Packages

```java
// ❌ BAD: Everything in module root
order/
├── OrderService.java
├── OrderRepository.java
├── OrderEntity.java
└── OrderMapper.java

// ✅ GOOD: Internal implementation hidden
order/
├── OrderService.java       # Public
├── Order.java             # Public DTO
└── internal/
    ├── OrderRepository.java
    ├── OrderEntity.java
    └── OrderMapper.java
```

### ❌ Mistake 3: Tight Coupling

```java
// ❌ BAD: Direct dependency
@Service
public class OrderService {
    private final PaymentService paymentService;
    
    public void createOrder(Order order) {
        saveOrder(order);
        paymentService.processPayment(order);  // Tight coupling
    }
}

// ✅ GOOD: Event-based
@Service
public class OrderService {
    private final ApplicationEventPublisher events;
    
    public void createOrder(Order order) {
        saveOrder(order);
        events.publishEvent(new OrderCreatedEvent(order));  // Loose coupling
    }
}
```

---

## 📚 When to Use

### ✅ Use Spring Modulith When:

- Starting a new medium/large Spring Boot project
- Refactoring a monolith for better structure
- Want domain-driven design without microservices
- Team wants clear boundaries but single deployment
- Not ready for microservices complexity

### ❌ Don't Use When:

- Very small applications (< 5 business domains)
- Already have working microservices
- Need independent scaling of components
- Different tech stacks per domain

---

## 📖 Module Dependencies

### Good Dependency Graph

```
config
  ↓
api → core
  ↓     ↓
impl  utils
```

### Bad Dependency Graph (Circular)

```
     ┌─────┐
     ↓     │
module-a → module-b
     ↑     │
     └─────┘
```

**Circular dependencies are violations!**

---

## 🛠️ Advanced Features

### Event Externalization

Persist events for reliability:

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
</dependency>
```

```java
// Events automatically persisted and retried on failure
@ApplicationModuleListener
@Transactional
public void handleOrder(OrderCreatedEvent event) {
    externalApi.notify(event);  // Retried if fails
}
```

### Observability

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-observability</artifactId>
</dependency>
```

Automatic metrics for:
- Module interactions
- Event processing times
- Failure rates

---

## 🎯 Cheat Sheet

| Task | Code |
|------|------|
| **Create module** | Create package next to main class |
| **Hide implementation** | Put in `internal/` sub-package |
| **Publish event** | `events.publishEvent(new MyEvent())` |
| **Listen to event** | `@ApplicationModuleListener void onEvent(MyEvent e)` |
| **Verify structure** | `ApplicationModules.of(App.class).verify()` |
| **Generate docs** | `new Documenter(modules).writeDocumentation()` |
| **Enable async** | `@EnableAsync` on main class |

---

## 📦 Useful Dependencies

```xml
<!-- Core -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-api</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Event persistence (optional) -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
</dependency>

<!-- Observability (optional) -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-observability</artifactId>
</dependency>
```

---

## 🔗 Resources

- **Docs:** https://docs.spring.io/spring-modulith/reference/
- **GitHub:** https://github.com/spring-projects/spring-modulith
- **Examples:** https://github.com/spring-projects/spring-modulith/tree/main/spring-modulith-examples
- **Tutorial:** https://www.baeldung.com/spring-modulith

---

## 💡 Pro Tips

1. **Start small:** Begin with 2-3 clear modules
2. **Use events:** Prefer events over direct dependencies
3. **Test boundaries:** Always run verification tests
4. **Document:** Generate diagrams for team visibility
5. **Hide internals:** Be strict about internal packages
6. **DTOs over entities:** Never expose JPA entities
7. **Async events:** Enable `@EnableAsync` for better performance

---

**Version:** Spring Modulith 1.2.2  
**Compatible with:** Spring Boot 3.x  
**Java:** 17+

---

## Need More Help?

- 📖 **Full Guide:** See `spring-modulith-guide.md`
- 🏗️ **Integration Plan:** See `2026-03-09-spring-modulith-integration-plan.md`
- 💬 **Questions:** Check GitHub Discussions

