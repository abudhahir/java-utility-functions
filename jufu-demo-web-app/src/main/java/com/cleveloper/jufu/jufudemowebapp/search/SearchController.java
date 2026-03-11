package com.cleveloper.jufu.jufudemowebapp.search;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.HeaderCondition;
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Demonstrates {@code request-utils} and {@code rule-factory} working together
 * in a single endpoint.
 *
 * <h2>How the two libraries compose</h2>
 * <ol>
 *   <li><strong>request-utils</strong> validates the HTTP protocol layer —
 *       {@code Content-Type} and {@code X-Subscription-Tier} headers —
 *       returning a {@link ConditionResult}.</li>
 *   <li><strong>rule-factory</strong> uses {@code ConditionResult::isMatched} as a
 *       {@code PredicateCondition} for its outer gate, then a domain predicate
 *       ({@link SearchRules#IS_PREMIUM}) for the inner tier-routing gate.</li>
 * </ol>
 *
 * <h2>Separation of concerns</h2>
 * <ul>
 *   <li>request-utils owns the <em>HTTP protocol layer</em>: header presence and format.</li>
 *   <li>Spring's {@code @RequestBody} owns body deserialisation (reading the stream once).</li>
 *   <li>rule-factory owns <em>domain routing</em>: tier selection and query validation
 *       against the already-parsed body.</li>
 * </ul>
 *
 * <h2>Decision tree</h2>
 * <pre>
 *   headers valid?  (request-utils — Content-Type + X-Subscription-Tier)
 *   ├── NO  → 400 Bad Request  (ConditionFailure messages from request-utils)
 *   └── YES → query not blank?  (rule-factory / SearchRules.QUERY_NOT_BLANK)
 *               ├── NO  → 400 Bad Request  (blank query message)
 *               └── YES → tier is premium?  (rule-factory / SearchRules.IS_PREMIUM)
 *                           ├── YES → 200 OK  full-content hits
 *                           └── NO  → 200 OK  snippet-only hits + upgrade hint
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Tiered search demonstrating request-utils + rule-factory integration")
public class SearchController {

    private final RequestConditionMatcher conditionMatcher;
    private final SearchService searchService;

    /**
     * HTTP protocol gates evaluated by request-utils.
     * Only reads headers — no body access — so it composes cleanly with Spring's @RequestBody.
     */
    private static final Condition HEADER_GATES = ConditionGroup.and(
            new HeaderCondition("Content-Type", "application/json", MatchOperation.CONTAINS, true),
            new HeaderCondition("X-Subscription-Tier", ".+", MatchOperation.REGEX, false)
    );

    @PostMapping
    @Operation(
            summary = "Tiered content search",
            description = """
                    Validates HTTP headers with request-utils, then routes the response
                    by subscription tier with rule-factory.

                    Required headers:
                    - Content-Type: application/json
                    - X-Subscription-Tier: premium | basic (or any other value)

                    Premium callers receive full content per hit.
                    Basic callers receive snippet-only results with an upgrade hint.
                    """,
            operationId = "search"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results — depth depends on subscription tier",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing/invalid header or blank query",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SearchResponse.class)))
    })
    public ResponseEntity<SearchResponse> search(
            HttpServletRequest httpReq,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Search query payload",
                    content = @Content(schema = @Schema(implementation = SearchRequest.class)))
            @RequestBody SearchRequest body
    ) {
        // ── Step 1: request-utils validates the HTTP protocol layer ──────────
        //   Reads headers only — body stream is left for @RequestBody above.
        ConditionResult headerCheck = conditionMatcher.evaluate(HEADER_GATES, httpReq);

        // Assemble domain context from the parsed body and validated header
        SearchContext ctx = new SearchContext(
                body.query(),
                httpReq.getHeader("X-Subscription-Tier")
        );

        // ── Step 2: rule-factory gates and routes the response ───────────────
        return PredicateResultFactory.selectLazy(

                // Gate 1 — are the required headers present and correctly formatted?
                //   ConditionResult::isMatched is the PredicateCondition<ConditionResult>.
                //   The request-utils result plugs directly into rule-factory — no adapter.
                ConditionResult::isMatched,
                headerCheck,

                // Headers valid → Gate 2: is the query non-blank?
                () -> PredicateResultFactory.selectLazy(
                        SearchRules.QUERY_NOT_BLANK,
                        ctx,

                        // Query present → Gate 3: route by subscription tier
                        () -> PredicateResultFactory.selectLazy(
                                SearchRules.IS_PREMIUM,
                                ctx,

                                // Premium: full content — supplier invoked only for premium callers
                                () -> ResponseEntity.ok(
                                        SearchResponse.full(ctx.query(), searchService.fullSearch(ctx.query()))
                                ),

                                // Basic: snippets only — supplier invoked only for non-premium callers
                                () -> ResponseEntity.ok(
                                        SearchResponse.limited(ctx.query(), searchService.basicSearch(ctx.query()))
                                )
                        ),

                        // Blank query → 400
                        () -> ResponseEntity.badRequest()
                                .body(SearchResponse.blankQuery())
                ),

                // Headers invalid → 400 with per-field violation messages from request-utils
                () -> ResponseEntity.badRequest()
                        .body(SearchResponse.invalid(headerCheck.getFailures()))
        );
    }
}
