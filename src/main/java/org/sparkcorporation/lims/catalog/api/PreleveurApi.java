package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Preleveur} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface PreleveurApi {

    Optional<PreleveurSummary> findById(PreleveurId id);

    Optional<PreleveurSummary> findByCode(PreleveurCode code);

    /** Throws {@code PreleveurNotFoundException} when no row matches. */
    PreleveurSummary requireById(PreleveurId id);

    boolean existsById(PreleveurId id);

    List<PreleveurSummary> findAllActive();
}
