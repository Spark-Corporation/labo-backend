package org.sparkcorporation.lims.billing.web;

import jakarta.validation.constraints.NotBlank;

/** Body for {@code POST /api/v1/invoices} — manual override (LAB_MANAGER) per spec §7.2. */
public record InvoiceRequest(@NotBlank String labFileId) {}
