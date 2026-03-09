package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Condition that matches specific fields in a JSON payload against a template.
 * Compares only the specified fields for exact equality.
 */
public class JsonExactMatchCondition implements Condition {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String template;
    private final String[] fields;

    public JsonExactMatchCondition(String template, String[] fields) {
        this.template = template;
        this.fields = fields;
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        Object jsonBody = context.getJsonBody();

        if (jsonBody == null) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonExactMatch")
                    .message("JSON body is null or not JSON")
                    .build()
            );
        }

        JsonNode templateNode;
        JsonNode requestNode;

        try {
            templateNode = OBJECT_MAPPER.readTree(template);
            requestNode = (JsonNode) jsonBody;
        } catch (Exception e) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonExactMatch")
                    .message(String.format("Error parsing JSON: %s", e.getMessage()))
                    .build()
            );
        }

        // Compare each specified field
        for (String field : fields) {
            JsonNode templateValue = templateNode.get(field);
            JsonNode requestValue = requestNode.get(field);

            if (templateValue == null) {
                return ConditionResult.failure(
                    ConditionFailure.builder()
                        .conditionType("JsonExactMatch")
                        .fieldName(field)
                        .message(String.format("Field '%s' not found in template", field))
                        .build()
                );
            }

            if (requestValue == null) {
                return ConditionResult.failure(
                    ConditionFailure.builder()
                        .conditionType("JsonExactMatch")
                        .fieldName(field)
                        .expectedValue(templateValue.asText())
                        .actualValue(null)
                        .message(String.format("Field '%s' is missing in request body", field))
                        .build()
                );
            }

            if (!templateValue.equals(requestValue)) {
                return ConditionResult.failure(
                    ConditionFailure.builder()
                        .conditionType("JsonExactMatch")
                        .fieldName(field)
                        .expectedValue(templateValue.asText())
                        .actualValue(requestValue.asText())
                        .message(String.format("Field '%s' expected to be '%s' but was '%s'",
                            field, templateValue.asText(), requestValue.asText()))
                        .build()
                );
            }
        }

        return ConditionResult.success();
    }
}
