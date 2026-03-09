package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationConditionParserTest {

    private final AnnotationConditionParser parser = new AnnotationConditionParser();

    @Test
    void shouldParseHeaderCondition() {
        JUFUCondition annotation = createHeaderAnnotation();

        ConditionGroup group = parser.parse(new JUFUCondition[]{annotation}, EvaluationMode.FAIL_FAST);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "premium");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = group.evaluate(context);
        assertTrue(result.isMatched());
    }

    @Test
    void shouldParseQueryParamCondition() {
        JUFUCondition annotation = createQueryParamAnnotation();

        ConditionGroup group = parser.parse(new JUFUCondition[]{annotation}, EvaluationMode.FAIL_FAST);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("version", "v2");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = group.evaluate(context);
        assertTrue(result.isMatched());
    }

    @Test
    void shouldParseMultipleConditionsAsGroup() {
        JUFUCondition headerAnnotation = createHeaderAnnotation();
        JUFUCondition queryAnnotation = createQueryParamAnnotation();

        ConditionGroup group = parser.parse(
            new JUFUCondition[]{headerAnnotation, queryAnnotation},
            EvaluationMode.FAIL_FAST
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "premium");
        request.setParameter("version", "v2");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = group.evaluate(context);
        assertTrue(result.isMatched());
    }

    // Helper methods to create annotation instances
    private JUFUCondition createHeaderAnnotation() {
        return new JUFUCondition() {
            @Override
            public Class<? extends Condition> value() {
                return Condition.class;
            }

            @Override
            public JUFUHeader header() {
                return new JUFUHeader() {
                    @Override
                    public String name() { return "X-Api-Key"; }
                    @Override
                    public String equals() { return "premium"; }
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
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUHeader.class;
                    }
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
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUQueryParam.class;
                    }
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
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUJsonPath.class;
                    }
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
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUJsonExactMatch.class;
                    }
                };
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return JUFUCondition.class;
            }
        };
    }

    private JUFUCondition createQueryParamAnnotation() {
        return new JUFUCondition() {
            @Override
            public Class<? extends Condition> value() {
                return Condition.class;
            }

            @Override
            public JUFUHeader header() {
                return new JUFUHeader() {
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
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUHeader.class;
                    }
                };
            }

            @Override
            public JUFUQueryParam queryParam() {
                return new JUFUQueryParam() {
                    @Override
                    public String name() { return "version"; }
                    @Override
                    public String equals() { return ""; }
                    @Override
                    public String contains() { return ""; }
                    @Override
                    public String startsWith() { return "v"; }
                    @Override
                    public String endsWith() { return ""; }
                    @Override
                    public String regex() { return ""; }
                    @Override
                    public boolean ignoreCase() { return false; }
                    @Override
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUQueryParam.class;
                    }
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
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUJsonPath.class;
                    }
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
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return JUFUJsonExactMatch.class;
                    }
                };
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return JUFUCondition.class;
            }
        };
    }
}
