package org.sparkcorporation.lims.patient.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

/** Patient's name components per CDC §I "nom, prénoms, nom de jeune fille". */
@Embeddable
public record PersonName(
        @Column(name = "last_name", length = 120) String lastName,
        @Column(name = "first_names", length = 160) @Nullable String firstNames,
        @Column(name = "maiden_name", length = 120) @Nullable String maidenName) {

    public PersonName {
        Objects.requireNonNull(lastName, "lastName");
        if (lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
    }
}
