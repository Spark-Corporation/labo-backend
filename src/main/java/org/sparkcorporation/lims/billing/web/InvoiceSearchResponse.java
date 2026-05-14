package org.sparkcorporation.lims.billing.web;

import java.util.List;

/** Page wrapper for {@code GET /api/v1/invoices}. */
public record InvoiceSearchResponse(List<InvoiceResponse> items, long total) {}
