package com.cleveloper.jufu.jufudemowebapp.user;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @TestConfiguration
    static class WebTestClientConfig {
        @Bean
        public WebTestClient webTestClient(WebApplicationContext context) {
            return MockMvcWebTestClient.bindToApplicationContext(context).build();
        }
    }

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig()
                .port(9999)
                .usingFilesUnderClasspath("wiremock")
        );
        wireMockServer.start();
    }

    @AfterAll
    static void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testListUsers() {
        webTestClient.get()
            .uri("/api/v1/users?page=0&size=10")
            .exchange()
            .expectStatus().isOk()
            .expectBody(PagedUserResponse.class)
            .value(response -> {
                assert response.getData().size() == 3;
                assert "u-101".equals(response.getData().get(0).getId());
                assert response.getPage() == 0;
                assert response.getTotal() == 3L;
            });
    }

    @Test
    void testGetUser() {
        webTestClient.get()
            .uri("/api/v1/users/u-101")
            .exchange()
            .expectStatus().isOk()
            .expectBody(UserDto.class)
            .value(user -> {
                assert "u-101".equals(user.getId());
                assert "alice@example.com".equals(user.getEmail());
                assert "ADMIN".equals(user.getRole());
            });
    }

    @Test
    void testGetUserNotFound() {
        webTestClient.get()
            .uri("/api/v1/users/unknown-id")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(ApiError.class)
            .value(error -> {
                assert "USER_NOT_FOUND".equals(error.getCode());
            });
    }

    @Test
    void testCreateUserDuplicateEmail() {
        CreateUserRequest request = CreateUserRequest.builder()
            .email("alice@example.com")
            .name("Test User")
            .role("USER")
            .build();

        webTestClient.post()
            .uri("/api/v1/users")
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .value(error -> {
                assert "EMAIL_CONFLICT".equals(error.getCode());
            });
    }

    @Test
    void testCreateUserInvalidRole() {
        CreateUserRequest request = CreateUserRequest.builder()
            .email("test@example.com")
            .name("Test User")
            .role("INVALID_ROLE")
            .build();

        webTestClient.post()
            .uri("/api/v1/users")
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody(ApiError.class)
            .value(error -> {
                assert "INVALID_ROLE".equals(error.getCode());
            });
    }

    @Test
    void testDeleteUser() {
        webTestClient.delete()
            .uri("/api/v1/users/u-101")
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    void testCorrelationIdHeader() {
        webTestClient.get()
            .uri("/api/v1/users/u-101")
            .header("X-Correlation-Id", "test-corr-123")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("X-Correlation-Id", "test-corr-123");
    }
}

