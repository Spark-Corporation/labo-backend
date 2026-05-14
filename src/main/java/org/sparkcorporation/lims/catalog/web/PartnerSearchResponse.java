package org.sparkcorporation.lims.catalog.web;

import java.util.List;

public record PartnerSearchResponse(List<PartnerResponse> items, long totalElements) {}
