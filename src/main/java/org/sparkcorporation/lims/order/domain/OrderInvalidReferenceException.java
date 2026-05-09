package org.sparkcorporation.lims.order.domain;

/**
 * Sealed base for the four cross-module reference invalidations the order service raises after
 * calling {@code *Api.existsById} (boolean) or {@code findByCode} (Optional) and getting a negative
 * answer. Mapped → 422 by a single {@code @ExceptionHandler(OrderInvalidReferenceException.class)}
 * in {@code OrderProblemDetailAdvice} (Chunk 5).
 *
 * <p>Per spec §8.2: order-domain owns this hierarchy because Modulith {@code allowedDependencies}
 * does not let order import patient/catalog {@code *NotFoundException} classes (those live in those
 * modules' {@code domain/}, outside the {@code api/} surface), and because the semantics of the
 * failure ("cross-module reference invalid for order intent") differ from the bare "entity not
 * found" the source modules expose.
 */
public abstract sealed class OrderInvalidReferenceException extends RuntimeException
        permits OrderInvalidPatientReferenceException,
                OrderInvalidPrescriberReferenceException,
                OrderInvalidPreleveurReferenceException,
                OrderInvalidAnalysisReferenceException {

    private final String referenceKind;
    private final String invalidValue;

    protected OrderInvalidReferenceException(String kind, String value, String detail) {
        super(detail);
        this.referenceKind = kind;
        this.invalidValue = value;
    }

    public String referenceKind() {
        return referenceKind;
    }

    public String invalidValue() {
        return invalidValue;
    }
}
