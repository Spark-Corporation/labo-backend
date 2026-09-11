package org.sparkcorporation.lims.billing.web;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body for {@code POST /api/v1/invoices} — manual override (LAB_MANAGER) per spec §7.2. */
@Schema(
        description =
                "Forces issuance of the invoice for a lab file that is already fully "
                        + "biologically validated but has not been auto-invoiced yet.")
public record InvoiceRequest(
        @Schema(description = "Id of the fully bio-validated lab file to invoice.") @NotBlank
                String labFileId) {}
