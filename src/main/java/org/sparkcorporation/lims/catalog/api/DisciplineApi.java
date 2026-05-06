package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Discipline} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface DisciplineApi {

    Optional<DisciplineSummary> findById(DisciplineId id);

    Optional<DisciplineSummary> findByCode(DisciplineCode code);

    /** Throws {@code DisciplineNotFoundException} when no row matches. */
    DisciplineSummary requireById(DisciplineId id);

    boolean existsById(DisciplineId id);

    List<DisciplineSummary> findAllActive();
}
