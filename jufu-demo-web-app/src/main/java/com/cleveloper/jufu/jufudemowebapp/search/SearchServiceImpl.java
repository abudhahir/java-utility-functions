package com.cleveloper.jufu.jufudemowebapp.search;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * In-memory implementation of {@link SearchService} backed by a seeded catalog.
 * Filters by case-insensitive match against title or snippet.
 */
@Service
class SearchServiceImpl implements SearchService {

    private static final List<SearchHit> CATALOG = List.of(
            new SearchHit(
                    "p-001",
                    "Spring Boot in Action",
                    "A comprehensive guide to building production-ready Spring Boot applications.",
                    "Chapter 1 — Getting started: Spring Boot makes it easy to create stand-alone, " +
                    "production-grade Spring-based applications. It takes an opinionated view of the " +
                    "Spring platform and third-party libraries so you can get started with minimum fuss."
            ),
            new SearchHit(
                    "p-002",
                    "Java 17 Records and Sealed Classes",
                    "Explore modern Java features including records, sealed classes, and pattern matching.",
                    "Records in Java 17 provide a compact syntax for declaring classes that are transparent " +
                    "holders for shallowly immutable data. Sealed classes restrict which other classes or " +
                    "interfaces may extend or implement them, giving you more control over your type hierarchy."
            ),
            new SearchHit(
                    "p-003",
                    "Reactive Programming with Spring WebFlux",
                    "Build non-blocking applications using Spring WebFlux and Project Reactor.",
                    "Spring WebFlux is a reactive-stack web framework added in Spring 5. It is fully " +
                    "non-blocking, supports Reactive Streams back pressure, and runs on servers such as " +
                    "Netty, Undertow, and Servlet containers. It coexists alongside Spring MVC."
            ),
            new SearchHit(
                    "p-004",
                    "Domain-Driven Design Patterns in Java",
                    "Apply DDD tactical patterns — aggregates, repositories, and value objects — in Java.",
                    "Domain-Driven Design is an approach that centres development on programming a domain " +
                    "model with a rich understanding of business processes and rules. Tactical patterns such " +
                    "as Aggregates, Entities, Value Objects, and Domain Events translate directly to Java."
            ),
            new SearchHit(
                    "p-005",
                    "Building REST APIs with Spring MVC",
                    "Design and implement REST APIs following best practices using Spring MVC.",
                    "REST (Representational State Transfer) is an architectural style for distributed " +
                    "hypermedia systems. Spring MVC provides a comprehensive model-view-controller framework " +
                    "for REST API development with built-in content negotiation and exception handling."
            )
    );

    @Override
    public List<SearchHit> fullSearch(String query) {
        String q = query.toLowerCase();
        return CATALOG.stream()
                .filter(h -> matches(h, q))
                .toList();
    }

    @Override
    public List<SearchHit> basicSearch(String query) {
        String q = query.toLowerCase();
        return CATALOG.stream()
                .filter(h -> matches(h, q))
                .map(h -> new SearchHit(h.id(), h.title(), h.snippet(), null))
                .toList();
    }

    private boolean matches(SearchHit hit, String lowercaseQuery) {
        return hit.title().toLowerCase().contains(lowercaseQuery)
                || hit.snippet().toLowerCase().contains(lowercaseQuery);
    }
}
