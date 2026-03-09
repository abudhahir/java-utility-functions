package com.cleveloper.jufu.requestutils.condition.config;

import com.cleveloper.jufu.requestutils.condition.core.RequestConditionMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConditionMatcherAutoConfigurationTest {

    @Autowired(required = false)
    private RequestConditionMatcher matcher;

    @Test
    void shouldAutoConfigureRequestConditionMatcher() {
        assertNotNull(matcher);
    }
}
