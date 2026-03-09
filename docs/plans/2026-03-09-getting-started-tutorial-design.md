# Getting Started Tutorial Design

**Date:** 2026-03-09
**Goal:** Create comprehensive, multi-file tutorial documentation for request-utils module
**Target Audience:** Spring Boot developers (experienced) + mixed audience with progressive depth
**Approach:** Hybrid - quick start, deep concepts, complete examples

---

## Overview

The tutorial will provide a progressive learning path from quick start to advanced usage, organized as separate markdown files for easy navigation and maintenance. Content targets Spring Boot developers who are new to request-utils, with depth that serves both quick-reference and deep-learning needs.

## Design Decisions

**Multi-file structure** chosen for:
- Easier navigation and bookmarking
- Independent updates to sections
- Clear learning progression
- Ability to reference specific sections

**Hybrid learning approach** provides:
- Quick start for immediate productivity
- Conceptual depth for understanding
- Complete examples for real-world application

**Comprehensive scope** covers:
- Core features (headers, params, conditions)
- Advanced features (JSON matching, AOP)
- Custom implementations
- Complete working examples

---

## File Structure

All files located under `request-utils/docs/tutorial/`:

### Core Tutorial Files

**00-index.md** - Landing page and navigation
- Tutorial overview and value proposition
- Learning path options (fast track, comprehensive, full course, problem-driven)
- Prerequisites checklist
- Visual navigation table with time estimates
- Additional resources

**01-quick-start.md** - 5-minute setup (~300 words)
- Zero fluff, straight to code
- Single complete example: dependency → inject → condition → evaluate
- Real use case: Premium user routing
- Quick win: "It works!"
- Next steps pointer

**02-core-concepts.md** - Foundation (~800 words)
- Architecture overview with visual diagram
- Four key interfaces: Condition, ConditionResult, ConditionFailure, RequestContext
- How RequestConditionMatcher works
- Match operations: EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, REGEX
- Evaluation modes: FAIL_FAST vs COLLECT_ALL
- Design philosophy: functional interfaces, immutability, explicit failures
- Minimal code snippets per concept

**03-headers-and-params.md** - Practical mastery (~1000 words)
- Organized by common scenarios, not API
- Scenarios: exact values, presence checks, pattern matching, case-insensitive
- Each scenario: why → code → result → pitfalls
- Side-by-side: Header vs QueryParam comparison
- Real examples: API keys, content negotiation, feature flags, versioning
- Troubleshooting section

**04-building-complex-conditions.md** - Complex logic (~900 words)
- AND logic with multi-factor auth example
- OR logic with device routing
- Nested groups with complex boolean expressions
- Builder API usage guidelines
- Evaluation mode selection criteria
- Anti-patterns and common mistakes
- Progressive examples: simple → moderate → complex
- Performance tips: condition ordering, early exits

### Advanced Topic Files

**05-json-matching.md** - JSON payload handling (~800 words)
- Prerequisites: json-path dependency setup
- Two approaches: JSONPath vs Exact Match
- JSONPath basics and syntax primer
- Common patterns: nested fields, arrays, complex objects
- Exact field matching use cases
- Combining JSON with other condition types
- Troubleshooting: path not found, null handling, invalid JSON
- Working example: Multi-tenant SaaS routing

**06-aop-annotations.md** - Declarative matching (~900 words)
- Prerequisites note (AOP included by default)
- Transformation: programmatic vs declarative comparison
- Method-level annotation usage
- Inline conditions vs class references
- Evaluation modes in annotations
- Request extraction mechanisms
- Exception handling with @ControllerAdvice
- Real example: Secured endpoint with multiple validations
- When NOT to use AOP
- Integration patterns

**07-custom-conditions.md** - Custom implementations (~1000 words)
- When to write custom conditions
- Deep dive: Condition interface and RequestContext
- Step-by-step implementation guide
- Example 1: Working hours condition (time-based)
- Example 2: IP whitelist condition
- Example 3: Rate limit condition (external service)
- Example 4: Composite business rule
- Building effective failure messages
- Stateful vs stateless design
- Testing patterns with mocking
- Using in groups and annotations
- Performance considerations: caching, lazy evaluation

### Reference & Examples

**08-complete-examples.md** - Full working scenarios (~1500 words)
- Complete, runnable examples with full context

**Example 1: API Versioning System**
- Multi-version REST API (v1, v2, v3)
- Complete controller code
- Version detection via header OR query param
- Fallback logic
- Shows: OR conditions, controller integration

**Example 2: Multi-Tenant SaaS Router**
- Tenant-specific routing
- Header + JSON payload validation
- Premium vs standard handling
- Service layer, data source selection
- Shows: JSON matching, complex groups, custom logic

**Example 3: Feature Flag System**
- Beta feature gating
- Multiple activation methods
- AOP-based automatic protection
- Auth integration
- Shows: AOP annotations, OR logic, configuration
- Complete application setup

**Example 4: Smart API Gateway**
- Internal vs external routing
- Dynamic authentication requirements
- Tiered rate limiting
- Shows: All features combined
- Complete working application with tests

Each example includes:
- Full file structure
- Complete code with imports
- Test cases
- Design decisions explanation
- Variations and extensions
- Common modifications

**09-troubleshooting.md** - Problem solving (~1200 words)
- Organized by symptom (how developers debug)

**"My condition isn't matching"**
- Diagnostic steps
- Common causes: typos, case mismatch, whitespace, wrong operation
- Debug techniques: COLLECT_ALL, print failures, inspect raw request
- Solutions with before/after code

**"NullPointerException or missing request"**
- AOP aspect errors
- When it happens: missing param, wrong scope, async issues
- RequestContextHolder explanation
- Solutions and workarounds

**"JSON matching not working"**
- PathNotFoundException handling
- Diagnostic checklist: dependency, content-type, JSON validity
- Common JSONPath mistakes
- Testing JSONPath expressions
- Configuration fixes

**"AOP aspect not triggering"**
- Annotation ignored scenarios
- Causes: missing AspectJ, AOP disabled, non-Spring bean
- Diagnostic steps: verify proxy, check configuration
- Configuration solutions

**"Performance issues"**
- Slow processing with many conditions
- Profiling expensive conditions
- Solutions: FAIL_FAST, reordering, caching
- Benchmarks and optimization guidance

**"ConditionNotMetException handling"**
- Catching and customizing responses
- Global exception handler patterns
- Structured error responses
- Logging and monitoring

**FAQ Section** (quick answers):
- Using outside Spring
- Nesting limits
- Thread safety
- Request modification
- Testing with annotations
- WebFlux support status

---

## Content Principles

**Progressive Disclosure:**
- Quick wins first
- Depth on demand
- Clear "what's next" pointers

**Show, Don't Just Tell:**
- Code-first examples
- Real-world scenarios
- Complete, runnable code

**Developer Experience:**
- Copy-paste ready snippets
- Clear error explanations
- Common pitfall warnings
- Performance guidance

**Navigation:**
- Multiple learning paths
- Cross-references between sections
- Clear prerequisites
- Time estimates for planning

---

## Learning Paths

**Fast Track (30 min):**
01 Quick Start → 08 Complete Examples
- For: Developers who learn by example
- Goal: Working code quickly

**Comprehensive (90 min):**
01 → 02 → 03 → 04 → 08
- For: Core feature mastery
- Goal: Solid foundation

**Full Course (3-4 hours):**
Sequential 01-09
- For: Complete understanding
- Goal: Master all features

**Problem-Driven (variable):**
Start at 09 Troubleshooting → Jump to relevant sections
- For: Debugging specific issues
- Goal: Quick problem resolution

---

## Success Metrics

Tutorial is successful if developers can:
1. Get first condition working in under 10 minutes (Quick Start)
2. Understand when to use each feature (Core Concepts)
3. Build complex routing logic confidently (Complex Conditions)
4. Integrate JSON and AOP when needed (Advanced Topics)
5. Solve problems independently (Troubleshooting)
6. Apply patterns to their specific use cases (Complete Examples)

---

## Implementation Notes

**File naming convention:**
- `00-` through `09-` for ordering
- Descriptive names matching section purpose
- `.md` extension

**Code examples:**
- Full imports included
- Spring Boot 4.0.3+ compatible
- Java 17 features used appropriately
- Tested and verified

**Cross-references:**
- Relative links between tutorial files
- Links to main README for API reference
- Links to example code directory
- Links to GitHub for support

**Maintenance:**
- Each file is independently updatable
- Version compatibility noted in examples
- Date last updated in each file
- Clear deprecation notices when features change

---

## Next Steps

1. Create tutorial directory structure
2. Implement each file sequentially
3. Create supporting examples directory
4. Test all code snippets
5. Review for consistency and clarity
6. Link from main README
7. Update project documentation index
