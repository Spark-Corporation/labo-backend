package org.sparkcorporation.lims.result.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import org.sparkcorporation.lims.result.api.ResultSummary;

@Schema(description = "Page of results in a validation/printing queue")
public record ResultQueueResponse(
        @Schema(description = "Results on this page") List<ResultSummary> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size") int size,
        @Schema(description = "Total number of results matching the queue, across all pages")
                long totalElements,
        @Schema(description = "Total number of pages") int totalPages) {

    public ResultQueueResponse {
        content = List.copyOf(content);
    }

    public static ResultQueueResponse from(Page<ResultSummary> page) {
        return new ResultQueueResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
