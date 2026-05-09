package org.sparkcorporation.lims.order.domain;

import org.jspecify.annotations.Nullable;

/**
 * Whitelist patch payload for {@link LabFile#update(UpdateLabFile, java.time.Clock)}. Each field is
 * nullable; a {@code null} sentinel means "no change". Only {@code urgent}, {@code prescriberId},
 * and {@code preleveurId} are mutable through this path — registration number, patient, and status
 * raise dedicated immutability exceptions higher up.
 */
public record UpdateLabFile(
        @Nullable Boolean urgent, @Nullable String prescriberId, @Nullable String preleveurId) {}
