package org.sparkcorporation.lims.result.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.Nullable;

/**
 * PATCH body for {@code PATCH /api/v1/results/{id}/lines/{lineId}}. Per spec §6.4 both fields are
 * independently nullable, but at least one must be present (controller rejects an empty {@code {}}
 * body with 400). The {@link ValueUnion} sub-record uses Jackson's typed-tagged union with a {@code
 * "kind"} discriminator carrying one of {@code INTEGER | REAL | TEXT | FORMULA | FREE}.
 *
 * <p>{@code RealVariant.value} is intentionally typed as {@code String} to preserve {@code
 * BigDecimal} precision through JSON deserialization (per spec §6.4 — string-encoded numeric).
 */
public record PatchLineRequest(@Valid @Nullable ValueUnion value, @Nullable String commentText) {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = ValueUnion.IntegerVariant.class, name = "INTEGER"),
        @JsonSubTypes.Type(value = ValueUnion.RealVariant.class, name = "REAL"),
        @JsonSubTypes.Type(value = ValueUnion.TextVariant.class, name = "TEXT"),
        @JsonSubTypes.Type(value = ValueUnion.FreeVariant.class, name = "FREE")
    })
    public sealed interface ValueUnion
            permits ValueUnion.IntegerVariant,
                    ValueUnion.RealVariant,
                    ValueUnion.TextVariant,
                    ValueUnion.FreeVariant {

        /** Variant for {@code ResultType.INTEGER}. */
        record IntegerVariant(@NotNull Long value) implements ValueUnion {}

        /** Variant for {@code ResultType.REAL}; {@code value} is string-encoded BigDecimal. */
        record RealVariant(
                @NotNull
                        @Pattern(
                                regexp = "^-?\\d+(\\.\\d+)?$",
                                message = "must be a valid decimal number")
                        String value)
                implements ValueUnion {}

        /** Variant for {@code ResultType.TEXT}. */
        record TextVariant(@NotNull String value) implements ValueUnion {}

        /** Variant for {@code ResultType.FREE} ("Autre"). */
        record FreeVariant(@NotNull String value) implements ValueUnion {}
    }
}
