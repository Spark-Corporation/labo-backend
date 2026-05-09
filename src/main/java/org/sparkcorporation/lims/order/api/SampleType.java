package org.sparkcorporation.lims.order.api;

/**
 * Sample provenance, used as JPA single-table-inheritance discriminator — see CDC §6.7-6.9 + spec
 * §3 / §4.4.
 */
public enum SampleType {
    /** Collected at the lab counter — no extra fields. */
    IN_LAB,
    /** Collected at patient address by a {@code Préleveur} — extra address/window/distance. */
    HOME,
    /** Received externally with a separate origin label — {@code collectedAt} is nullable. */
    EXTERNAL_RECEIVED
}
