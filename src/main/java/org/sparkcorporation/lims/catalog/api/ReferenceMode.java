package org.sparkcorporation.lims.catalog.api;

/**
 * Reference-interval rendering mode per CDC §8 onglet "Intervalles de référence" — verbatim list
 * "Standard, Commentaire, Aucun".
 *
 * <ul>
 *   <li>{@link #STANDARD} — numeric min/max bounds in unit1 (and optionally unit2).
 *   <li>{@link #COMMENT} — free-text commentary; no numeric bounds.
 *   <li>{@link #NONE} — no reference interval applies (e.g. qualitative results).
 * </ul>
 */
public enum ReferenceMode {
    STANDARD,
    COMMENT,
    NONE
}
