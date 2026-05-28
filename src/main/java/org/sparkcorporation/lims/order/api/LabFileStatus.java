package org.sparkcorporation.lims.order.api;

/**
 * Lifecycle states of a LabFile (dossier) — see spec §5.1. 1.4 only operates on {@link #OPEN} (any
 * mutation when {@code status != OPEN} raises {@code LabFileNotEditableException}); the other
 * states are reserved for sub-projects 1.5 (results) and beyond.
 */
public enum LabFileStatus {
    OPEN,
    AWAITING_RESULTS,
    TECHNICALLY_VALIDATED,
    BIOLOGICALLY_VALIDATED,
    SIGNED,
    PUBLISHED
}
