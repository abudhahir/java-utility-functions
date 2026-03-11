package com.cleveloper.jufu.jufudemowebapp.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User resource")
public class UserDto {
    @Schema(description = "Unique user identifier", example = "u-101")
    private String id;

    @Schema(description = "User's email address", example = "alice@example.com")
    private String email;

    @Schema(description = "User's full name", example = "Alice Smith")
    private String name;

    @Schema(description = "User role", example = "ADMIN", allowableValues = {"ADMIN", "USER"})
    private String role;

    @Schema(description = "User status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
    private String status;
}

