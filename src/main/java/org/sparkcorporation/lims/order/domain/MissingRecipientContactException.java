package org.sparkcorporation.lims.order.domain;

/**
 * Mapped → 422 by {@code OrderProblemDetailAdvice}. Thrown when a requested channel has no usable
 * recipient address on the patient's contact record (no email for {@code EMAIL}; no phone — and no
 * guardian-phone fallback — for {@code WHATSAPP}).
 *
 * <p>Owned by {@code order}; the {@code delivery} module defines its own sibling. The two modules
 * do not share this type.
 */
public class MissingRecipientContactException extends RuntimeException {

    public MissingRecipientContactException(String detail) {
        super(detail);
    }
}
