package com.cleveloper.jufu.jufudemowebapp.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserBackendClient backendClient;

    @GetMapping
    @Operation(
        summary = "List users",
        description = "Retrieve a paginated list of users",
        operationId = "listUsers"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedUserResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PagedUserResponse> listUsers(
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") Integer page,
        @Parameter(description = "Page size", example = "10")
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(backendClient.listUsers(page, size));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a user by their unique identifier",
        operationId = "getUser"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserDto> getUser(
        @Parameter(description = "User ID", example = "u-101")
        @PathVariable String id
    ) {
        return ResponseEntity.ok(backendClient.getUser(id));
    }

    @PostMapping
    @Operation(
        summary = "Create user",
        description = "Create a new user",
        operationId = "createUser"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "422", description = "Invalid user data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(backendClient.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update user",
        description = "Update an existing user",
        operationId = "updateUser"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "422", description = "Invalid user data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserDto> updateUser(
        @Parameter(description = "User ID", example = "u-101")
        @PathVariable String id,
        @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(backendClient.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user",
        description = "Delete a user",
        operationId = "deleteUser"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "User ID", example = "u-101")
        @PathVariable String id
    ) {
        backendClient.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

