package org.sparkcorporation.lims.catalog.web;

import java.util.List;

public record DisciplineSearchResponse(
        List<DisciplineResponse> content, int page, int size, long totalElements, int totalPages) {}
