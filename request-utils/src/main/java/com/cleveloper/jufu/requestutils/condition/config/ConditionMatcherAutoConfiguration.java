package com.cleveloper.jufu.requestutils.condition.config;

import com.cleveloper.jufu.requestutils.condition.aop.AnnotationConditionParser;
import com.cleveloper.jufu.requestutils.condition.aop.ConditionMatchingAspect;
import com.cleveloper.jufu.requestutils.condition.core.RequestConditionMatcher;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for request condition matching.
 * Registers the RequestConditionMatcher, AnnotationConditionParser, and ConditionMatchingAspect as Spring beans.
 */
@AutoConfiguration
public class ConditionMatcherAutoConfiguration {

    @Bean
    public RequestConditionMatcher requestConditionMatcher() {
        return new RequestConditionMatcher();
    }

    @Bean
    public AnnotationConditionParser annotationConditionParser() {
        return new AnnotationConditionParser();
    }

    @Bean
    @ConditionalOnClass(Aspect.class)
    public ConditionMatchingAspect conditionMatchingAspect(AnnotationConditionParser parser) {
        return new ConditionMatchingAspect(parser);
    }
}
