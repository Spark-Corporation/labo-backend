package org.sparkcorporation.lims.patient.domain;

import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Patient's identity document (CDC §I "Numéro de Carte d'identité, ou de passeport"). */
@Embeddable
public record IdDocument(@Enumerated(EnumType.STRING) IdDocumentType type, String number) {

    public IdDocument {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(number, "number");
        if (number.isBlank()) {
            throw new IllegalArgumentException("number must not be blank");
        }
    }
}
