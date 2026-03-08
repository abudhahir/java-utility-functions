package com.cleveloper.jufu.requestutils.condition.config;

import com.cleveloper.jufu.requestutils.condition.core.RequestConditionMatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for request condition matching.
 * Registers the RequestConditionMatcher as a Spring bean.
 */
@AutoConfiguration
public class ConditionMatcherAutoConfiguration {

    @Bean
    public RequestConditionMatcher requestConditionMatcher() {
        return new RequestConditionMatcher();
    }
}
