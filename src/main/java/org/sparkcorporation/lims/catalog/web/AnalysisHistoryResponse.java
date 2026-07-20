package org.sparkcorporation.lims.catalog.web;

import java.util.List;

/** Paginated envelope for an analysis's configuration-modification history (SP12). */
public record AnalysisHistoryResponse(
        List<AnalysisHistoryEntry> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
