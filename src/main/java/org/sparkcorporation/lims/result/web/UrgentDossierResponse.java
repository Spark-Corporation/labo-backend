package org.sparkcorporation.lims.result.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(description = "Page of dossiers flagged urgent")
public record UrgentDossierResponse(
        @Schema(description = "Urgent dossiers on this page") List<DossierRow> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size") int size,
        @Schema(description = "Total number of urgent dossiers, across all pages")
                long totalElements,
        @Schema(description = "Total number of pages") int totalPages) {

    public UrgentDossierResponse {
        content = List.copyOf(content);
    }

    public static UrgentDossierResponse from(Page<DossierRow> page) {
        return new UrgentDossierResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
