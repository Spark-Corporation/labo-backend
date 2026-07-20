package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

public record HomeCollectionFeeSearchResponse(
        List<HomeCollectionFeeResponse> items, long totalElements) {}
