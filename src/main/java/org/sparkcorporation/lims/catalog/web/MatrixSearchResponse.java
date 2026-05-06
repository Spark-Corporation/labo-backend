package org.sparkcorporation.lims.catalog.web;

import java.util.List;

public record MatrixSearchResponse(
        List<MatrixResponse> content, int page, int size, long totalElements, int totalPages) {}
