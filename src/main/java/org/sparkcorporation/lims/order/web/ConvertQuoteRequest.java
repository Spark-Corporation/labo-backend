package org.sparkcorporation.lims.order.web;

/**
 * POST {@code /api/v1/quotes/{id}:convert} payload — passes the {@code urgent} flag through to the
 * implicit {@code LabFileService.create} call (spec §5.5).
 */
public record ConvertQuoteRequest(boolean urgent) {}
