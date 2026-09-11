package org.sparkcorporation.lims.result.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * Page envelope for {@link OverdueDossierRow}. Shape mirrors {@link UrgentDossierResponse} for
 * frontend consistency across the {@code /queues/*} surface family.
 */
@Schema(description = "Page of dossiers with at least one analysis past its turnaround deadline")
public record OverdueDossierResponse(
        @Schema(description = "Overdue dossiers on this page") List<OverdueDossierRow> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size") int size,
        @Schema(description = "Total number of overdue dossiers, across all pages")
                long totalElements,
        @Schema(description = "Total number of pages") int totalPages) {

    public OverdueDossierResponse {
        content = List.copyOf(content);
    }

    public static OverdueDossierResponse from(Page<OverdueDossierRow> p) {
        return new OverdueDossierResponse(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages());
    }
}
