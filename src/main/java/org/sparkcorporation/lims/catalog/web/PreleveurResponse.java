package org.sparkcorporation.lims.catalog.web;

import java.time.Instant;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.catalog.api.Habilitation;

public record PreleveurResponse(
        String id,
        String code,
        String name,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String userId,
        Set<Habilitation> habilitations,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
