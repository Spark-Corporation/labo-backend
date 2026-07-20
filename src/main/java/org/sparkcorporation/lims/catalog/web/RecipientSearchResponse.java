package org.sparkcorporation.lims.catalog.web;

import java.util.List;

public record RecipientSearchResponse(
        List<RecipientResponse> content, int page, int size, long totalElements, int totalPages) {}
