package com.cleveloper.jufu.requestutils.condition.builder;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ConditionGroupBuilderTest {

    @Test
    void shouldBuildSimpleAndGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(ctx -> ConditionResult.success())
            .and(ctx -> ConditionResult.success())
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldBuildSimpleOrGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .or(ctx -> ConditionResult.success())
            .or(ctx -> ConditionResult.failure(
                ConditionFailure.builder().message("Failed").build()
            ))
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldBuildNestedAndGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(ctx -> ConditionResult.success())
            .andGroup(builder -> builder
                .and(ctx -> ConditionResult.success())
                .and(ctx -> ConditionResult.success())
            )
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldBuildNestedOrGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(ctx -> ConditionResult.success())
            .orGroup(builder -> builder
                .or(ctx -> ConditionResult.success())
                .or(ctx -> ConditionResult.failure(
                    ConditionFailure.builder().message("Failed").build()
                ))
            )
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldSetEvaluationMode() {
        ConditionGroup group = ConditionGroup.builder()
            .mode(EvaluationMode.COLLECT_ALL)
            .and(ctx -> ConditionResult.success())
            .build();

        assertNotNull(group);
    }
}
