package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Recipient} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface RecipientApi {

    Optional<RecipientSummary> findById(RecipientId id);

    Optional<RecipientSummary> findByCode(RecipientCode code);

    /** Throws {@code RecipientNotFoundException} when no row matches. */
    RecipientSummary requireById(RecipientId id);

    boolean existsById(RecipientId id);

    List<RecipientSummary> findAllActive();
}
