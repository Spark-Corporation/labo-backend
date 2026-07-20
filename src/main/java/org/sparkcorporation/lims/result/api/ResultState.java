package org.sparkcorporation.lims.result.api;

/**
 * 4-state machine vocabulary for the {@code Result} aggregate, per spec §4.5.
 *
 * <p>Order is preserved across releases. {@link #ordinal()} is NOT used as a transition guard —
 * domain logic compares enum values explicitly to keep the transition rules readable.
 *
 * <ul>
 *   <li>{@link #DRAFT} — at least one {@code ResultLine.value*} is null. Transitions to/from {@link
 *       #ENTERED} are computed automatically on every line save.
 *   <li>{@link #ENTERED} — all expected lines have a value. Awaiting technician's "Valider
 *       techniquement" button (CDC §IV).
 *   <li>{@link #TECHNICAL_VALIDATED} — technician has validated. Awaiting biologist's "Valider
 *       biologiquement" button (CDC §IV / §X).
 *   <li>{@link #BIOLOGICAL_VALIDATED} — terminal. Publishes {@code BiologicalValidationDone} which
 *       triggers downstream billing / delivery / stats.
 * </ul>
 */
public enum ResultState {
    DRAFT,
    ENTERED,
    TECHNICAL_VALIDATED,
    BIOLOGICAL_VALIDATED
}
