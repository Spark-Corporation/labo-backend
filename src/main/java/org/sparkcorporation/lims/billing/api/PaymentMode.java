package org.sparkcorporation.lims.billing.api;

/**
 * Payment mode enum per CDC §VIII. Mirrored in the DB-side {@code payments_mode_chk} CHECK
 * constraint; updates to this enum require a matching Liquibase changeset to widen the constraint.
 *
 * <ul>
 *   <li>{@code CASH} — espèces.
 *   <li>{@code CHEQUE} — chèque bancaire.
 *   <li>{@code CARD_TPE} — carte via terminal de paiement (TPE).
 *   <li>{@code FLOOZ} — Moov Africa mobile money (Togo / Bénin).
 *   <li>{@code MIXX_BY_YAS} — Yas / Togocom mobile money.
 * </ul>
 */
public enum PaymentMode {
    CASH,
    CHEQUE,
    CARD_TPE,
    FLOOZ,
    MIXX_BY_YAS
}
