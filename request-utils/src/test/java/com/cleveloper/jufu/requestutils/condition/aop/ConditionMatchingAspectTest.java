package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.core.EvaluationMode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ConditionMatchingAspectTest {

    private final AnnotationConditionParser parser = new AnnotationConditionParser();
    private final ConditionMatchingAspect aspect = new ConditionMatchingAspect(parser);

    @Test
    void shouldProceedWhenConditionMatches() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "premium");

        JUFUMatchConditions annotation = createAnnotation("premium");

        // Should not throw exception when condition matches
        assertDoesNotThrow(() -> aspect.checkConditions(null, annotation, request));
    }

    @Test
    void shouldThrowExceptionWhenConditionFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "basic");

        JUFUMatchConditions annotation = createAnnotation("premium");

        // Should throw exception when condition fails
        assertThrows(ConditionNotMetException.class,
            () -> aspect.checkConditions(null, annotation, request));
    }

    private JUFUMatchConditions createAnnotation(String expectedValue) {
        return new JUFUMatchConditions() {
            @Override
            public JUFUCondition[] value() {
                return new JUFUCondition[]{
                    new JUFUCondition() {
                        @Override
                        public Class annotationType() {
                            return JUFUCondition.class;
                        }

                        @Override
                        public Class value() {
                            return com.cleveloper.jufu.requestutils.condition.core.Condition.class;
                        }

                        @Override
                        public JUFUHeader header() {
                            return new JUFUHeader() {
                                @Override
                                public String name() { return "X-Api-Key"; }
                                @Override
                                public String equals() { return expectedValue; }
                                @Override
                                public String contains() { return ""; }
                                @Override
                                public String startsWith() { return ""; }
                                @Override
                                public String endsWith() { return ""; }
                                @Override
                                public String regex() { return ""; }
                                @Override
                                public boolean ignoreCase() { return false; }
                                @Override
                                public Class annotationType() { return JUFUHeader.class; }
                            };
                        }

                        @Override
                        public JUFUQueryParam queryParam() {
                            return new JUFUQueryParam() {
                                @Override
                                public String name() { return ""; }
                                @Override
                                public String equals() { return ""; }
                                @Override
                                public String contains() { return ""; }
                                @Override
                                public String startsWith() { return ""; }
                                @Override
                                public String endsWith() { return ""; }
                                @Override
                                public String regex() { return ""; }
                                @Override
                                public boolean ignoreCase() { return false; }
                                @Override
                                public Class annotationType() { return JUFUQueryParam.class; }
                            };
                        }

                        @Override
                        public JUFUJsonPath jsonPath() {
                            return new JUFUJsonPath() {
                                @Override
                                public String path() { return ""; }
                                @Override
                                public String equals() { return ""; }
                                @Override
                                public String contains() { return ""; }
                                @Override
                                public String startsWith() { return ""; }
                                @Override
                                public String endsWith() { return ""; }
                                @Override
                                public String regex() { return ""; }
                                @Override
                                public boolean ignoreCase() { return false; }
                                @Override
                                public Class annotationType() { return JUFUJsonPath.class; }
                            };
                        }

                        @Override
                        public JUFUJsonExactMatch jsonExactMatch() {
                            return new JUFUJsonExactMatch() {
                                @Override
                                public String template() { return ""; }
                                @Override
                                public String[] fields() { return new String[]{}; }
                                @Override
                                public Class annotationType() { return JUFUJsonExactMatch.class; }
                            };
                        }
                    }
                };
            }

            @Override
            public EvaluationMode mode() {
                return EvaluationMode.FAIL_FAST;
            }

            @Override
            public Class annotationType() {
                return JUFUMatchConditions.class;
            }
        };
    }
}
