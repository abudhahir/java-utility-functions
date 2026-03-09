package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.core.EvaluationMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for AOP-based condition matching.
 * Tests the complete flow from controller annotation to aspect execution.
 * <p>
 * This test uses a standalone MockMvc setup with manual aspect and parser configuration,
 * simulating the full AOP integration that would occur in a Spring Boot application.
 */
class AopIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Create the parser and aspect
        AnnotationConditionParser parser = new AnnotationConditionParser();
        ConditionMatchingAspect aspect = new ConditionMatchingAspect(parser);

        // Build standalone MockMvc setup with aspect-enabled controller
        TestController controller = new TestController();
        AspectEnabledTestController aspectController = new AspectEnabledTestController(controller, aspect);

        mockMvc = MockMvcBuilders
            .standaloneSetup(aspectController)
            .setControllerAdvice(new ConditionExceptionHandler())
            .build();
    }

    @Test
    void shouldAllowRequestWhenConditionMatches() throws Exception {
        mockMvc.perform(get("/test/header-check")
                .header("X-API-Key", "valid-key"))
            .andExpect(status().isOk())
            .andExpect(content().string("Success"));
    }

    @Test
    void shouldRejectRequestWhenHeaderConditionFails() throws Exception {
        mockMvc.perform(get("/test/header-check")
                .header("X-API-Key", "invalid-key"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectRequestWhenQueryParamConditionFails() throws Exception {
        mockMvc.perform(get("/test/query-check")
                .param("version", "v1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowRequestWhenQueryParamMatches() throws Exception {
        mockMvc.perform(get("/test/query-check")
                .param("version", "v2"))
            .andExpect(status().isOk())
            .andExpect(content().string("Success"));
    }

    @Test
    void shouldAllowRequestWhenJsonPathMatches() throws Exception {
        String jsonBody = "{\"user\":{\"role\":\"admin\"}}";

        mockMvc.perform(post("/test/json-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
            .andExpect(status().isOk())
            .andExpect(content().string("Success"));
    }

    @Test
    void shouldRejectRequestWhenJsonPathFails() throws Exception {
        String jsonBody = "{\"user\":{\"role\":\"guest\"}}";

        mockMvc.perform(post("/test/json-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowRequestWhenMultipleConditionsMatch() throws Exception {
        mockMvc.perform(get("/test/multi-check")
                .header("X-API-Key", "valid-key")
                .param("version", "v2"))
            .andExpect(status().isOk())
            .andExpect(content().string("Success"));
    }

    @Test
    void shouldRejectRequestWhenAnyConditionFailsInAndGroup() throws Exception {
        // Valid header but invalid query param
        mockMvc.perform(get("/test/multi-check")
                .header("X-API-Key", "valid-key")
                .param("version", "v1"))
            .andExpect(status().isForbidden());
    }

    /**
     * Exception handler for ConditionNotMetException.
     * Returns 403 Forbidden when conditions are not met.
     */
    @ControllerAdvice
    static class ConditionExceptionHandler {

        @ExceptionHandler(ConditionNotMetException.class)
        public ResponseEntity<String> handleConditionNotMet(ConditionNotMetException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Condition not met: " + ex.getMessage());
        }
    }

    /**
     * Test controller with various @JUFUMatchConditions annotations.
     * Methods define the conditions that should be met before execution.
     */
    static class TestController {

        /**
         * Endpoint with header condition check.
         */
        @JUFUMatchConditions(value = {
            @JUFUCondition(header = @JUFUHeader(
                name = "X-API-Key",
                equals = "valid-key"
            ))
        })
        public String headerCheck() {
            return "Success";
        }

        /**
         * Endpoint with query parameter condition check.
         */
        @JUFUMatchConditions(value = {
            @JUFUCondition(queryParam = @JUFUQueryParam(
                name = "version",
                equals = "v2"
            ))
        })
        public String queryCheck() {
            return "Success";
        }

        /**
         * Endpoint with JSON path condition check.
         */
        @JUFUMatchConditions(value = {
            @JUFUCondition(jsonPath = @JUFUJsonPath(
                path = "$.user.role",
                equals = "admin"
            ))
        })
        public String jsonCheck() {
            return "Success";
        }

        /**
         * Endpoint with multiple conditions (AND logic).
         */
        @JUFUMatchConditions(
            value = {
                @JUFUCondition(header = @JUFUHeader(
                    name = "X-API-Key",
                    equals = "valid-key"
                )),
                @JUFUCondition(queryParam = @JUFUQueryParam(
                    name = "version",
                    equals = "v2"
                ))
            },
            mode = EvaluationMode.FAIL_FAST
        )
        public String multiCheck() {
            return "Success";
        }
    }

    /**
     * Wrapper controller that manually invokes the aspect before delegating to the test controller.
     * This simulates AOP behavior in a standalone MockMvc test environment.
     */
    @RestController
    @RequestMapping("/test")
    static class AspectEnabledTestController {

        private final TestController delegate;
        private final ConditionMatchingAspect aspect;

        AspectEnabledTestController(TestController delegate, ConditionMatchingAspect aspect) {
            this.delegate = delegate;
            this.aspect = aspect;
        }

        @GetMapping("/header-check")
        public String headerCheck(jakarta.servlet.http.HttpServletRequest request) throws Throwable {
            JUFUMatchConditions annotation = TestController.class
                .getMethod("headerCheck")
                .getAnnotation(JUFUMatchConditions.class);
            aspect.checkConditions(null, annotation, request);
            return delegate.headerCheck();
        }

        @GetMapping("/query-check")
        public String queryCheck(jakarta.servlet.http.HttpServletRequest request) throws Throwable {
            JUFUMatchConditions annotation = TestController.class
                .getMethod("queryCheck")
                .getAnnotation(JUFUMatchConditions.class);
            aspect.checkConditions(null, annotation, request);
            return delegate.queryCheck();
        }

        @PostMapping("/json-check")
        public String jsonCheck(jakarta.servlet.http.HttpServletRequest request) throws Throwable {
            JUFUMatchConditions annotation = TestController.class
                .getMethod("jsonCheck")
                .getAnnotation(JUFUMatchConditions.class);
            aspect.checkConditions(null, annotation, request);
            return delegate.jsonCheck();
        }

        @GetMapping("/multi-check")
        public String multiCheck(jakarta.servlet.http.HttpServletRequest request) throws Throwable {
            JUFUMatchConditions annotation = TestController.class
                .getMethod("multiCheck")
                .getAnnotation(JUFUMatchConditions.class);
            aspect.checkConditions(null, annotation, request);
            return delegate.multiCheck();
        }
    }
}
