package com.cleveloper.jufu.jufudemowebapp.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated user list response")
public class PagedUserResponse {
    @Schema(description = "List of users in current page")
    private List<UserDto> data;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private Integer page;

    @Schema(description = "Page size", example = "10")
    private Integer size;

    @Schema(description = "Total number of users", example = "3")
    private Long total;
}

