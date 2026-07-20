package org.sparkcorporation.lims.billing.api;

/**
 * Customer-routing discriminator for an {@link InvoiceSummary}. Mirrors the {@code customer_kind}
 * CHECK on {@code invoices} (spec §5.1).
 */
public enum InvoiceCustomerKind {
    PATIENT,
    INSURANCE,
    PARTNER
}
