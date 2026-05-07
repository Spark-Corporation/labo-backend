package org.sparkcorporation.lims.catalog.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for {@link org.sparkcorporation.lims.catalog.domain.Analysis}. */
public record AnalysisId(String value) {

    public AnalysisId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static AnalysisId generate() {
        return new AnalysisId(Ulid.generate());
    }

    public static AnalysisId of(String value) {
        return new AnalysisId(value);
    }
}
