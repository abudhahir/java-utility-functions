package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.JUFUMatchConditions;
import com.cleveloper.jufu.requestutils.condition.core.ConditionGroup;
import com.cleveloper.jufu.requestutils.condition.core.ConditionResult;
import com.cleveloper.jufu.requestutils.condition.core.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that intercepts methods annotated with @JUFUMatchConditions
 * and evaluates conditions before method execution.
 */
@Aspect
@Order(100)
public class ConditionMatchingAspect {

    private final AnnotationConditionParser parser;

    public ConditionMatchingAspect(AnnotationConditionParser parser) {
        this.parser = parser;
    }

    /**
     * Intercept methods annotated with @JUFUMatchConditions.
     * Evaluates conditions before method execution and throws exception if they fail.
     */
    @Before("@annotation(matchConditions)")
    public void checkConditions(JoinPoint joinPoint, JUFUMatchConditions matchConditions) {
        // Extract HttpServletRequest from method arguments or RequestContextHolder
        HttpServletRequest request = extractRequest(joinPoint);

        // Call the public method that does the actual checking
        checkConditions(joinPoint, matchConditions, request);
    }

    /**
     * Public method for testing purposes that performs the actual condition checking.
     */
    public void checkConditions(JoinPoint joinPoint, JUFUMatchConditions matchConditions, HttpServletRequest request) {
        // Parse annotations into ConditionGroup
        ConditionGroup conditionGroup = parser.parse(matchConditions.value(), matchConditions.mode());

        // Create RequestContext from HttpServletRequest
        RequestContext context = RequestContext.from(request);

        // Evaluate conditions
        ConditionResult result = conditionGroup.evaluate(context);

        // Throw exception if conditions not met
        if (!result.isMatched()) {
            throw new ConditionNotMetException(result);
        }
    }

    /**
     * Extract HttpServletRequest from method arguments or Spring's RequestContextHolder.
     */
    private HttpServletRequest extractRequest(JoinPoint joinPoint) {
        // First, try to find HttpServletRequest in method arguments
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }

        // If not found in arguments, try RequestContextHolder
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException(
                "No HttpServletRequest found in method arguments or RequestContextHolder. " +
                "Method annotated with @JUFUMatchConditions must have HttpServletRequest parameter " +
                "or be executed in a web request context."
            );
        }

        return attributes.getRequest();
    }
}
