package org.sparkcorporation.lims.iam.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One page of staff user search results.")
public record UserSearchResponse(
        @Schema(description = "Users on this page.") List<UserResponse> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Requested page size.") int size,
        @Schema(description = "Total number of matching users across all pages.")
                long totalElements,
        @Schema(description = "Total number of pages.") int totalPages) {}
