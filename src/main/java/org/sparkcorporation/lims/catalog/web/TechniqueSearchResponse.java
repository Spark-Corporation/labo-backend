package org.sparkcorporation.lims.catalog.web;

import java.util.List;

public record TechniqueSearchResponse(
        List<TechniqueResponse> content, int page, int size, long totalElements, int totalPages) {}
