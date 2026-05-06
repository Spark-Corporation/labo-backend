package org.sparkcorporation.lims.catalog.web;

import java.util.List;

public record AntibioticSearchResponse(
        List<AntibioticResponse> content, int page, int size, long totalElements, int totalPages) {}
