package org.sparkcorporation.lims.result.web;

import java.util.List;

import org.springframework.data.domain.Page;

public record UrgentDossierResponse(
        List<DossierRow> content, int page, int size, long totalElements, int totalPages) {

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
