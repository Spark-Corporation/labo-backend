package org.sparkcorporation.lims.result.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(description = "Page of result-line search hits matching the requested filters")
public record ResultSearchResponse(
        @Schema(description = "Matching result lines on this page") List<ResultSearchRow> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size") int size,
        @Schema(description = "Total number of matching result lines, across all pages")
                long totalElements,
        @Schema(description = "Total number of pages") int totalPages) {

    public ResultSearchResponse {
        content = List.copyOf(content);
    }

    public static ResultSearchResponse from(Page<ResultSearchRow> page) {
        return new ResultSearchResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
