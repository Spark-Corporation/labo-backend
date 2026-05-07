package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record PrescriberResponse(
        String id,
        String code,
        @Nullable String title,
        String name,
        @Nullable SpecialtySnapshotResponse specialty,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String licenseNumber,
        @Nullable String institution,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    /** Flat snapshot of the linked specialty embedded in the prescriber response. */
    public record SpecialtySnapshotResponse(String id, String code, String name) {}
}
