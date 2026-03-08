package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.core.ConditionResult;
import java.util.stream.Collectors;

/**
 * Exception thrown when request conditions are not met.
 * Contains detailed information about which conditions failed.
 */
public class ConditionNotMetException extends RuntimeException {
    private final ConditionResult result;

    public ConditionNotMetException(ConditionResult result) {
        super(formatMessage(result));
        this.result = result;
    }

    public ConditionResult getResult() {
        return result;
    }

    private static String formatMessage(ConditionResult result) {
        if (result.getFailures().isEmpty()) {
            return "Request conditions not met";
        }

        if (result.getFailures().size() == 1) {
            return "Request condition not met: " + result.getFailures().get(0).getMessage();
        }

        return "Request conditions not met:\n" +
            result.getFailures().stream()
                .map(f -> "  - " + f.toString())
                .collect(Collectors.joining("\n"));
    }
}
