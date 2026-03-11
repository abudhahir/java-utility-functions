# Rule Factory Tutorial

**Eliminate Repetitive if/else — Select Values with Predicates**

Welcome to the comprehensive tutorial for `rule-factory`, a Java 17 utility for predicate-driven two-outcome selection. This tutorial takes you from zero to production-ready code in under an hour.

---

## What is rule-factory?

`rule-factory` provides a focused, type-safe API for choosing one of two values (or computing one of two results) based on a predicate condition — without writing repetitive `if/else` blocks.

**Perfect for:**
- Selecting response DTOs based on user role
- Choosing strategy objects from runtime conditions
- Deferring expensive object creation to only the needed branch
- Building domain-readable conditional rules
- Replacing scattered ternary expressions with named predicates

---

## Who is This For?

This tutorial is designed for **Java 17 developers** who want clean, testable conditional selection in their code. We assume you're comfortable with:
- Java 17 basics
- Functional interfaces and lambdas
- `java.util.function.Predicate` and `Supplier`

---

## What You'll Learn

- ✅ **Quick Start** (5 min) — Your first factory call
- ✅ **Core Concepts** (15 min) — The two operations and the predicate contract
- ✅ **Eager Selection** (20 min) — Select from pre-built values
- ✅ **Lazy Selection** (20 min) — Defer construction with suppliers
- ✅ **Custom Predicates** (25 min) — Domain-specific functional interfaces
- ✅ **Complete Examples** (40 min) — Real-world applications
- ✅ **Troubleshooting** (15 min) — Common pitfalls and fixes

**Total time:** 2–3 hours depending on depth

---

## Learning Paths

Choose the path that fits your needs:

### 🚀 Fast Track (20 minutes)
**Goal:** Get working code quickly  
**Path:** [01 Quick Start](01-quick-start.md) → [06 Complete Examples](06-complete-examples.md)  
**Best for:** Learning by example, immediate results

### 📚 Core Track (60 minutes)
**Goal:** Understand both selection modes  
**Path:** [01](01-quick-start.md) → [02](02-core-concepts.md) → [03](03-eager-selection.md) → [04](04-lazy-selection.md)  
**Best for:** Production usage, understanding trade-offs

### 🎓 Full Course (2–3 hours)
**Goal:** Complete mastery  
**Path:** Sequential 01 → 02 → 03 → 04 → 05 → 06 → 07  
**Best for:** Team training, library contributors

### 🔧 Problem-Driven (variable)
**Goal:** Fix a specific issue  
**Path:** Start at [07 Troubleshooting](07-troubleshooting.md) → jump to relevant section  
**Best for:** Debugging existing code

---

## Tutorial Contents

| # | Topic | Level | Time | Description |
|---|-------|-------|------|-------------|
| [01](01-quick-start.md) | Quick Start | Beginner | 5 min | First factory call |
| [02](02-core-concepts.md) | Core Concepts | Beginner | 15 min | Architecture and key types |
| [03](03-eager-selection.md) | Eager Selection | Intermediate | 20 min | Value-based selection patterns |
| [04](04-lazy-selection.md) | Lazy Selection | Intermediate | 20 min | Supplier-based deferred selection |
| [05](05-custom-predicates.md) | Custom Predicates | Advanced | 25 min | Domain-specific functional interfaces |
| [06](06-complete-examples.md) | Complete Examples | All | 50 min | Real-world applications (4 examples) |
| [07](07-troubleshooting.md) | Troubleshooting | All | 15 min | Diagnose and fix common issues |

---

## Prerequisites

Before starting, ensure you have:
- [ ] Java 17 or later installed
- [ ] Maven 3.8+ available
- [ ] `rule-factory` module on your classpath (or built locally)
- [ ] Basic familiarity with Java functional interfaces

---

## Two-Minute Overview

`rule-factory` provides two static methods:

```java
// Eager: choose from two pre-built values
R result = PredicateResultFactory.select(predicate, input, whenTrue, whenFalse);

// Lazy: compute only the selected branch
R result = PredicateResultFactory.selectLazy(predicate, input, trueSupplier, falseSupplier);
```

That's the entire API surface. The rest of this tutorial shows you when, why, and how to use each form effectively.

---

**→ Ready? Start with [Quick Start](01-quick-start.md)**

