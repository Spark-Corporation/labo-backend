package org.sparkcorporation.lims.patient.domain;

import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.jspecify.annotations.Nullable;

/** CDC §I — "Contact de l'accompagnant, du parent ou tuteur légal". Optional on Patient. */
@Embeddable
public record Guardian(
        String name,
        @Nullable String phone,
        @Enumerated(EnumType.STRING) GuardianRelationship relationship) {

    public Guardian {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("guardian name must not be blank");
        }
        Objects.requireNonNull(relationship, "relationship");
    }
}
