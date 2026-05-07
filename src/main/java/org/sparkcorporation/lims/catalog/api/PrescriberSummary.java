package org.sparkcorporation.lims.catalog.api;

import org.jspecify.annotations.Nullable;

public record PrescriberSummary(
        PrescriberId id,
        String code,
        @Nullable String title,
        String name,
        @Nullable SpecialtySnapshot specialty,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String licenseNumber,
        @Nullable String institution) {

    /** Flat snapshot of the linked specialty at read time — avoids cross-module N+1. */
    public record SpecialtySnapshot(SpecialtyId id, String code, String name) {}
}
