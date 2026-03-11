package com.cleveloper.jufu.jufudemowebapp.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new user")
public class CreateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "User's email address", example = "bob@example.com")
    private String email;

    @NotBlank(message = "Name is required")
    @Schema(description = "User's full name", example = "Bob Johnson")
    private String name;

    @NotBlank(message = "Role is required")
    @Schema(description = "User role", example = "USER", allowableValues = {"ADMIN", "USER"})
    private String role;
}

