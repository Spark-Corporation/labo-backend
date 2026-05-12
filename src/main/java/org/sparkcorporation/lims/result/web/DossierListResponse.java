package org.sparkcorporation.lims.result.web;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Paginated envelope for the §IX dossier-list endpoint ({@code GET /api/v1/dossiers}). Distinct
 * from {@link UrgentDossierResponse} per standing convention — each cluster gets its own response
 * type even though the shape is identical.
 */
public record DossierListResponse(
        List<DossierRow> content, int page, int size, long totalElements, int totalPages) {

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
