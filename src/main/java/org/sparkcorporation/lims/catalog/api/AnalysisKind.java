package org.sparkcorporation.lims.catalog.api;

/**
 * Discriminator for the {@code Analysis} aggregate.
 *
 * <p>Values per CDC §4 (Bilan société), §8 (Analyses simples), §9 (Analyses groupées). Order is
 * preserved across releases; UIs may iterate {@link #values()} for display.
 */
public enum AnalysisKind {
    SIMPLE,
    GROUPED,
    SOCIETY_PANEL
}
