package com.cleveloper.jufu.requestutils.condition.annotations;

import com.cleveloper.jufu.requestutils.condition.core.Condition;
import java.lang.annotation.*;

/**
 * Individual condition definition.
 * Supports class reference mode, inline mode, or hybrid.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(JUFUMatchConditions.class)
public @interface JUFUCondition {

    /**
     * Condition class to instantiate (class reference mode).
     */
    Class<? extends Condition> value() default Condition.class;

    /**
     * Inline header condition.
     */
    JUFUHeader header() default @JUFUHeader(name = "");

    /**
     * Inline query parameter condition.
     */
    JUFUQueryParam queryParam() default @JUFUQueryParam(name = "");
}
