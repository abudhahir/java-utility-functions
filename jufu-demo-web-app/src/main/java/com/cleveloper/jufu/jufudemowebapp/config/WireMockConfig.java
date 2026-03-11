package com.cleveloper.jufu.jufudemowebapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * WireMock configuration for embedded mock backend.
 * Note: WireMock is only used in test profiles.
 * For runtime, ensure the backend at demo.backend.base-url is available.
 */
@Configuration
@Slf4j
public class WireMockConfig implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("WireMock configuration loaded (uses classpath:/wiremock mappings in tests)");
    }
}


