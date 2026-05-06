package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Antibiotic} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface AntibioticApi {

    Optional<AntibioticSummary> findById(AntibioticId id);

    Optional<AntibioticSummary> findByCode(AntibioticCode code);

    /** Throws {@code AntibioticNotFoundException} when no row matches. */
    AntibioticSummary requireById(AntibioticId id);

    boolean existsById(AntibioticId id);

    List<AntibioticSummary> findAllActive();
}
