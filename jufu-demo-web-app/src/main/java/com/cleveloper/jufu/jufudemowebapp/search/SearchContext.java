package com.cleveloper.jufu.jufudemowebapp.search;

/**
 * Domain context assembled by the controller from the request body and headers.
 * Passed to {@link SearchRules} predicates for tier-based routing.
 */
public record SearchContext(String query, String tier) {}
