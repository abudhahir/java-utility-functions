package com.cleveloper.jufu.requestutils.condition.annotations;

import com.cleveloper.jufu.requestutils.condition.core.EvaluationMode;
import java.lang.annotation.*;

/**
 * Container annotation for multiple conditions.
 * Can be placed on methods or classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface JUFUMatchConditions {

    /**
     * Array of conditions to evaluate.
     */
    JUFUCondition[] value();

    /**
     * Evaluation mode for these conditions.
     */
    EvaluationMode mode() default EvaluationMode.FAIL_FAST;
}
