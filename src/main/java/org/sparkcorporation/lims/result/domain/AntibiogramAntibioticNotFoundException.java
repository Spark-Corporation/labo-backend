package org.sparkcorporation.lims.result.domain;

import org.sparkcorporation.lims.catalog.api.AntibioticId;

/**
 * Thrown when a referenced {@code Antibiotic} cannot be located in the catalog at antibiogram
 * reading time. Distinct from the catalog-side {@code AntibioticNotFoundException} (which extends
 * {@code EntityNotFoundException} → 404 for direct catalog lookups); this result-side surface maps
 * to {@code 422 antibiotic.not-found} via {@code ResultProblemDetailAdvice} (chunk 5) because here
 * the missing antibiotic is a foreign-key violation surfaced by a result mutation, not a primary
 * lookup.
 */
public class AntibiogramAntibioticNotFoundException extends RuntimeException {

    private final AntibioticId antibioticId;

    public AntibiogramAntibioticNotFoundException(AntibioticId id) {
        super("Antibiotic " + id.value() + " does not exist");
        this.antibioticId = id;
    }

    public AntibioticId antibioticId() {
        return antibioticId;
    }
}
