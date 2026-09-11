package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Paginated envelope for an analysis's configuration-modification history (SP12). */
@Schema(description = "Paginated timeline of configuration changes recorded for an analysis.")
public record AnalysisHistoryResponse(
        @Schema(description = "History entries on this page, most recent revision first.")
                List<AnalysisHistoryEntry> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Requested page size.") int size,
        @Schema(description = "Total number of history entries across all pages.")
                long totalElements,
        @Schema(description = "Total number of pages.") int totalPages) {}
