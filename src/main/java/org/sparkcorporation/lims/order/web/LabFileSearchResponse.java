package org.sparkcorporation.lims.order.web;

import java.util.List;

/** Paginated GET {@code /api/v1/lab-files} response envelope. */
public record LabFileSearchResponse(
        List<LabFileResponse> content, int page, int size, long totalElements, int totalPages) {

    public LabFileSearchResponse {
        content = List.copyOf(content);
    }
}
