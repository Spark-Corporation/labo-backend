package org.sparkcorporation.lims.catalog.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /**
     * Batched lookup. Returns a map keyed by id; ids not present in the database are omitted from
     * the result (callers handle absence). Empty input returns an empty map without issuing a
     * query.
     *
     * <p>Matches the contract of {@link AnalysisApi#findAllByIds} on shape. Unlike the analysis
     * variant, this method does NOT filter soft-deleted rows — it matches the lenient behavior of
     * {@link #findById(AntibioticId)} so the result CR renderer can still resolve the code of a
     * historical antibiotic that has since been soft-deleted from the catalog.
     *
     * <p>Use this instead of looping over {@link #findById(AntibioticId)} when reading multiple ids
     * (e.g. when rendering an antibiogram block in a report).
     */
    Map<AntibioticId, AntibioticSummary> findAllByIds(Collection<AntibioticId> ids);
}
