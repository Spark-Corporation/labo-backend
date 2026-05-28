package org.sparkcorporation.lims.iam.infra;

import java.time.Instant;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.jspecify.annotations.Nullable;

/**
 * Persists an {@link Instant} as its canonical ISO-8601 string ({@link Instant#toString()}) rather
 * than a SQL {@code timestamptz}.
 *
 * <p>This is load-bearing for the signing chain: secured-x's {@code CanonicalJson} serializes a
 * {@code SignedAction}'s timestamp via {@code Instant.toString()} and the Ed25519 signature is
 * computed over those exact bytes. A {@code timestamptz} column only stores microsecond precision,
 * so an {@code Instant.now()} captured at nanosecond precision (JDK 9+) would round-trip lossily and
 * the persisted-then-reloaded action would fail signature verification. Storing the ISO string makes
 * the round-trip byte-identical ({@code Instant.parse(i.toString()).equals(i)} always holds).
 */
@Converter(autoApply = false)
public class InstantIso8601Converter implements AttributeConverter<Instant, String> {

    @Override
    public @Nullable String convertToDatabaseColumn(@Nullable Instant attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public @Nullable Instant convertToEntityAttribute(@Nullable String dbData) {
        return dbData == null ? null : Instant.parse(dbData);
    }
}
