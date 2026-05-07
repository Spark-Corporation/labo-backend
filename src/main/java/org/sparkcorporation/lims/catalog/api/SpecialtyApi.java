package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Specialty} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface SpecialtyApi {

    Optional<SpecialtySummary> findById(SpecialtyId id);

    Optional<SpecialtySummary> findByCode(SpecialtyCode code);

    /** Throws {@code SpecialtyNotFoundException} when no row matches. */
    SpecialtySummary requireById(SpecialtyId id);

    boolean existsById(SpecialtyId id);

    List<SpecialtySummary> findAllActive();
}
