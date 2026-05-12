package org.sparkcorporation.lims.result.web;

/**
 * §V {@code resultState} query-param values per CDC §V line 155 ("vides, à contrôler, urgents,
 * validés, non validés"). The {@link #URGENT} value maps to the orthogonal {@code lab_files.urgent}
 * flag rather than a result-state band — see spec §6.5 divergence note.
 */
public enum SearchResultState {
    EMPTY,
    TO_CHECK,
    URGENT,
    VALIDATED,
    NOT_VALIDATED
}
