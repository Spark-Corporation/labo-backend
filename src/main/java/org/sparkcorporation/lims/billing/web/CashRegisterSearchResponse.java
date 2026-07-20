package org.sparkcorporation.lims.billing.web;

import java.util.List;

/** Wrapper for {@code GET /api/v1/cash-registers}. */
public record CashRegisterSearchResponse(List<CashRegisterResponse> items, long totalElements) {}
