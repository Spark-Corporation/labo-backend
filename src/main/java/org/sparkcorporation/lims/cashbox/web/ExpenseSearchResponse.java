package org.sparkcorporation.lims.cashbox.web;

import java.util.List;

public record ExpenseSearchResponse(List<ExpenseResponse> items, long totalElements) {}
