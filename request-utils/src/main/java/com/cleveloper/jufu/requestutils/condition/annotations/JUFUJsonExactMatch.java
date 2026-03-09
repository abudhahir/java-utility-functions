package com.cleveloper.jufu.requestutils.condition.annotations;

import java.lang.annotation.*;

/**
 * Inline exact JSON field matching configuration.
 * Compares specific fields in the request JSON against a template.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JUFUJsonExactMatch {

    /**
     * JSON template string containing expected field values.
     * Example: "{\"type\": \"premium\", \"region\": \"US\"}"
     */
    String template();

    /**
     * Array of field names to compare between template and request.
     * Only these fields will be matched; other fields are ignored.
     */
    String[] fields();
}
