package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Prescriber} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface PrescriberApi {

    Optional<PrescriberSummary> findById(PrescriberId id);

    Optional<PrescriberSummary> findByCode(PrescriberCode code);

    /** Throws {@code PrescriberNotFoundException} when no row matches. */
    PrescriberSummary requireById(PrescriberId id);

    boolean existsById(PrescriberId id);

    List<PrescriberSummary> findAllActive();
}
