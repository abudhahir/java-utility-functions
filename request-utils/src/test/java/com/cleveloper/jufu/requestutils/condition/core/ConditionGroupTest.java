package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ConditionGroupTest {

    @Test
    void shouldEvaluateAndGroupWithAllPassing() {
        Condition condition1 = ctx -> ConditionResult.success();
        Condition condition2 = ctx -> ConditionResult.success();

        ConditionGroup group = ConditionGroup.and(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldEvaluateAndGroupWithOneFailing() {
        Condition condition1 = ctx -> ConditionResult.success();
        Condition condition2 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed").build()
        );

        ConditionGroup group = ConditionGroup.and(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());
    }

    @Test
    void shouldEvaluateOrGroupWithOnePassing() {
        Condition condition1 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed").build()
        );
        Condition condition2 = ctx -> ConditionResult.success();

        ConditionGroup group = ConditionGroup.or(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldEvaluateOrGroupWithAllFailing() {
        Condition condition1 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed 1").build()
        );
        Condition condition2 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed 2").build()
        );

        ConditionGroup group = ConditionGroup.or(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertFalse(result.isMatched());
        assertEquals(2, result.getFailures().size());
    }

    @Test
    void shouldSupportNestedGroups() {
        Condition c1 = ctx -> ConditionResult.success();
        Condition c2 = ctx -> ConditionResult.success();
        Condition c3 = ctx -> ConditionResult.success();

        // (c1 OR c2) AND c3
        ConditionGroup orGroup = ConditionGroup.or(c1, c2);
        ConditionGroup andGroup = ConditionGroup.and(orGroup, c3);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = andGroup.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }
}
