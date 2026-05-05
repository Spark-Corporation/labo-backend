package org.sparkcorporation.lims.patient.domain;

import java.util.Objects;

import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

/** Patient's name components per CDC §I "nom, prénoms, nom de jeune fille". */
@Embeddable
public record PersonName(
        String lastName, @Nullable String firstNames, @Nullable String maidenName) {

    public PersonName {
        Objects.requireNonNull(lastName, "lastName");
        if (lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
    }
}
