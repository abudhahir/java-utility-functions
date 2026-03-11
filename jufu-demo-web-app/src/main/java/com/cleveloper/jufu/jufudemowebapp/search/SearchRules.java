package com.cleveloper.jufu.jufudemowebapp.search;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;

/**
 * Domain predicate constants for the search feature.
 *
 * <p>Each constant is a named {@link PredicateCondition} that can be passed
 * directly to {@code PredicateResultFactory.selectLazy()} for response routing.
 */
public final class SearchRules {

    private SearchRules() {}

    /**
     * True when the caller's subscription tier is {@code premium} (case-insensitive).
     * Drives the choice between full-content and snippet-only search results.
     */
    public static final PredicateCondition<SearchContext> IS_PREMIUM =
            ctx -> "premium".equalsIgnoreCase(ctx.tier());

    /**
     * True when the search query is non-null and not blank.
     * Applied after the request body has been parsed by Spring — no re-reading needed.
     */
    public static final PredicateCondition<SearchContext> QUERY_NOT_BLANK =
            ctx -> ctx.query() != null && !ctx.query().isBlank();
}
