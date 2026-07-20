package org.sparkcorporation.lims.billing.domain;

import jakarta.persistence.EntityNotFoundException;

import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.order.api.LabFileId;

/**
 * Raised when an invoice lookup misses. Extends {@link EntityNotFoundException} so the shared
 * {@code ProblemDetailAdvice} routes it to {@code 404 Not Found}.
 */
public class InvoiceNotFoundException extends EntityNotFoundException {

    public InvoiceNotFoundException(String message) {
        super(message);
    }

    public static InvoiceNotFoundException forId(InvoiceId id) {
        return new InvoiceNotFoundException("invoice not found: " + id.value());
    }

    public static InvoiceNotFoundException forLabFile(LabFileId labFileId) {
        return new InvoiceNotFoundException("lab file not found: " + labFileId.value());
    }
}
