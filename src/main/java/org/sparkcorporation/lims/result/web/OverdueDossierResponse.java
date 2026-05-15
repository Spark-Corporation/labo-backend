package org.sparkcorporation.lims.result.web;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Page envelope for {@link OverdueDossierRow}. Shape mirrors {@link UrgentDossierResponse} for
 * frontend consistency across the {@code /queues/*} surface family.
 */
public record OverdueDossierResponse(
        List<OverdueDossierRow> content, int page, int size, long totalElements, int totalPages) {

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
