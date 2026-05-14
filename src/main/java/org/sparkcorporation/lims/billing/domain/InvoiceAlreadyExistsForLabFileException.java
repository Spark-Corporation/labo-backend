package org.sparkcorporation.lims.billing.domain;

import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Raised by the manual-override path ({@code POST /api/v1/invoices}) when an invoice has already
 * been issued for the given lab file. The auto-finalization listener never throws this — it detects
 * the existing row via {@code findByLabFileId} and returns silently (idempotency, spec §8.1). HTTP
 * mapping: 409 Conflict.
 */
public class InvoiceAlreadyExistsForLabFileException extends RuntimeException {

    private final LabFileId labFileId;
    private final InvoiceId existingInvoiceId;

    public InvoiceAlreadyExistsForLabFileException(
            LabFileId labFileId, InvoiceId existingInvoiceId) {
        super(
                "invoice already issued for lab file "
                        + labFileId.value()
                        + " (invoiceId="
                        + existingInvoiceId.value()
                        + ")");
        this.labFileId = labFileId;
        this.existingInvoiceId = existingInvoiceId;
    }

    public LabFileId labFileId() {
        return labFileId;
    }

    public InvoiceId existingInvoiceId() {
        return existingInvoiceId;
    }
}
