package org.sparkcorporation.lims.catalog.api;

/**
 * Result rendering / validation discriminator on the {@code Analysis} aggregate.
 *
 * <p>Values per CDC §8 onglet "Type de résultat" — verbatim list "Formule, Entier, Réel, Texte,
 * Autre". {@link #FREE} maps to "Autre". Order preserved across releases.
 *
 * <p>The aggregate stores the discriminator only; result-value semantics are 1.5 result scope.
 */
public enum ResultType {
    FORMULA,
    INTEGER,
    REAL,
    TEXT,
    FREE
}
