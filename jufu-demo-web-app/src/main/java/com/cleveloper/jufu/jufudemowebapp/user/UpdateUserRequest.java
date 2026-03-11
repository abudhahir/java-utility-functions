package com.cleveloper.jufu.jufudemowebapp.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update a user")
public class UpdateUserRequest {
    @Email(message = "Email must be valid")
    @Schema(description = "User's email address", example = "bob.updated@example.com")
    private String email;

    @Schema(description = "User's full name", example = "Bob Johnson Updated")
    private String name;

    @Schema(description = "User role", example = "ADMIN", allowableValues = {"ADMIN", "USER"})
    private String role;

    @Schema(description = "User status", example = "INACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
    private String status;
}

