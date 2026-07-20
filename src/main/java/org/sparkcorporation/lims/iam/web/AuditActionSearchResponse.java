package org.sparkcorporation.lims.iam.web;

import java.util.List;

/** Paginated GET {@code /api/v1/audit/actions} response envelope. */
public record AuditActionSearchResponse(
        List<AuditActionResponse> content, int page, int size, long totalElements, int totalPages) {

    public AuditActionSearchResponse {
        content = List.copyOf(content);
    }
}
