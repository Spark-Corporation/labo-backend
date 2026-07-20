package org.sparkcorporation.lims.catalog.web;

import java.util.List;

public record InsuranceSearchResponse(List<InsuranceResponse> items, long totalElements) {}
