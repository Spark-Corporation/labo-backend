package org.sparkcorporation.lims.result.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * Paginated envelope for the §IX dossier-list endpoint ({@code GET /api/v1/dossiers}). Distinct
 * from {@link UrgentDossierResponse} per standing convention — each cluster gets its own response
 * type even though the shape is identical.
 */
@Schema(description = "Page of dossiers matching the requested filters")
public record DossierListResponse(
        @Schema(description = "Dossiers on this page") List<DossierRow> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size") int size,
        @Schema(description = "Total number of dossiers matching the filters, across all pages")
                long totalElements,
        @Schema(description = "Total number of pages") int totalPages) {

    public DossierListResponse {
        content = List.copyOf(content);
    }

    public static DossierListResponse from(Page<DossierRow> page) {
        return new DossierListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
