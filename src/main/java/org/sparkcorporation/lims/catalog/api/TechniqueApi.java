package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Technique} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface TechniqueApi {

    Optional<TechniqueSummary> findById(TechniqueId id);

    Optional<TechniqueSummary> findByCode(TechniqueCode code);

    /** Throws {@code TechniqueNotFoundException} when no row matches. */
    TechniqueSummary requireById(TechniqueId id);

    boolean existsById(TechniqueId id);

    List<TechniqueSummary> findAllActive();
}
