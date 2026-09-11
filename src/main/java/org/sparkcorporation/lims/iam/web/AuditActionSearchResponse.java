package org.sparkcorporation.lims.iam.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Paginated GET {@code /api/v1/audit/actions} response envelope. */
@Schema(description = "One page of signed-action audit ledger entries.")
public record AuditActionSearchResponse(
        @Schema(description = "Audit ledger entries on this page.")
                List<AuditActionResponse> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Requested page size.") int size,
        @Schema(description = "Total number of matching entries across all pages.")
                long totalElements,
        @Schema(description = "Total number of pages.") int totalPages) {

    public AuditActionSearchResponse {
        content = List.copyOf(content);
    }
}
