package org.sparkcorporation.lims.order.api;

import java.util.Objects;

import org.sparkcorporation.lims.shared.id.Ulid;

/** Typed ULID for an {@code AnalysisRequest} on a {@link LabFileId} dossier. */
public record AnalysisRequestId(String value) {

    public AnalysisRequestId {
        Objects.requireNonNull(value, "value");
        if (!Ulid.isValid(value)) {
            throw new IllegalArgumentException("invalid ULID: " + value);
        }
    }

    public static AnalysisRequestId next() {
        return new AnalysisRequestId(Ulid.generate());
    }
}
