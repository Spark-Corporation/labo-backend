package org.sparkcorporation.lims.catalog.api;

import java.util.Set;

import org.jspecify.annotations.Nullable;

public record PreleveurSummary(
        PreleveurId id,
        String code,
        String name,
        @Nullable String phone,
        @Nullable String email,
        @Nullable String userId,
        Set<Habilitation> habilitations) {}
