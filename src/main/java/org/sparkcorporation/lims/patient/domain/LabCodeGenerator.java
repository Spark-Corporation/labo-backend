package org.sparkcorporation.lims.patient.domain;

import java.time.LocalDate;

/**
 * Generator of {@link LabCode} values. Composes a configured {@link LabCodePrefix} with the given
 * day and an atomic counter sequence into the {@code <prefix>-<yyMMdd>-<seq>} format.
 *
 * <p>Implementations must produce unique codes per call and roll back with the surrounding
 * transaction.
 */
public interface LabCodeGenerator {

    LabCode next(LocalDate day);
}
