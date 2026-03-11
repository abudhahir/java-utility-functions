package com.cleveloper.jufu.jufudemowebapp.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserBackendClient {

    @Value("${demo.backend.base-url:http://localhost:9999}")
    private String backendBaseUrl;

    private final RestClient restClient;

    public PagedUserResponse listUsers(Integer page, Integer size) {
        try {
            return restClient.get()
                .uri(backendBaseUrl + "/users?page={page}&size={size}", page, size)
                .retrieve()
                .body(PagedUserResponse.class);
        } catch (RestClientResponseException e) {
            log.error("Error listing users: {}", e.getMessage());
            throw new BackendException("Failed to list users", e);
        }
    }

    public UserDto getUser(String id) {
        try {
            return restClient.get()
                .uri(backendBaseUrl + "/users/{id}", id)
                .retrieve()
                .body(UserDto.class);
        } catch (RestClientResponseException e) {
            handleError(e);
            throw new BackendException("Failed to get user", e);
        }
    }

    public UserDto createUser(CreateUserRequest request) {
        try {
            return restClient.post()
                .uri(backendBaseUrl + "/users")
                .body(request)
                .retrieve()
                .body(UserDto.class);
        } catch (RestClientResponseException e) {
            handleError(e);
            throw new BackendException("Failed to create user", e);
        }
    }

    public UserDto updateUser(String id, UpdateUserRequest request) {
        try {
            return restClient.put()
                .uri(backendBaseUrl + "/users/{id}", id)
                .body(request)
                .retrieve()
                .body(UserDto.class);
        } catch (RestClientResponseException e) {
            handleError(e);
            throw new BackendException("Failed to update user", e);
        }
    }

    public void deleteUser(String id) {
        try {
            restClient.delete()
                .uri(backendBaseUrl + "/users/{id}", id)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            handleError(e);
            throw new BackendException("Failed to delete user", e);
        }
    }

    private void handleError(RestClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();
        log.error("Backend error ({}): {}", statusCode, responseBody);
    }

    static class BackendException extends RuntimeException {
        BackendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

