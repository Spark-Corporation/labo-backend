package org.sparkcorporation.lims.patient.domain;

import java.time.LocalDate;

/**
 * Atomic counter for patient lab codes. The counter is keyed by {@code (prefix, day)} and
 * monotonically increases on each call. The first call for a given key returns 1.
 *
 * <p>Implementations must guarantee atomicity under concurrent calls — two simultaneous {@link
 * #incrementAndGet(LabCodePrefix, LocalDate)} invocations from different transactions must produce
 * distinct sequence numbers.
 */
public interface LabCodeCounter {

    int incrementAndGet(LabCodePrefix prefix, LocalDate day);
}
