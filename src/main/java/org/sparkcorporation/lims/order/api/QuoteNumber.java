package org.sparkcorporation.lims.order.api;

import java.util.regex.Pattern;

import jakarta.persistence.Embeddable;

/**
 * Typed business-key for {@code Quote.quoteNumber} per CDC §III "Générer automatiquement un numéro
 * de devis". Format {@code DEV-yyMMdd-NNNN} (4-digit zero-padded sequence, per-day, no discipline).
 *
 * <p>Annotated {@code @Embeddable} so it can be embedded into the {@code Quote} aggregate via
 * {@code @Embedded} + {@code @AttributeOverride(name = "value", column = ...)} (precedent: {@link
 * LabFileRegistrationNumber}).
 */
@Embeddable
public record QuoteNumber(String value) {

    private static final Pattern FORMAT = Pattern.compile("^DEV-\\d{6}-\\d{4}$");

    public QuoteNumber {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid quote number: " + value);
        }
    }
}
