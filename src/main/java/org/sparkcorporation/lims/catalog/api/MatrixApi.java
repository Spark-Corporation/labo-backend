package org.sparkcorporation.lims.catalog.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-module read-only iface for the {@code Matrix} aggregate. Other modules import {@code
 * catalog.api} only.
 */
public interface MatrixApi {

    Optional<MatrixSummary> findById(MatrixId id);

    Optional<MatrixSummary> findByCode(MatrixCode code);

    /** Throws {@code MatrixNotFoundException} when no row matches. */
    MatrixSummary requireById(MatrixId id);

    boolean existsById(MatrixId id);

    List<MatrixSummary> findAllActive();
}
