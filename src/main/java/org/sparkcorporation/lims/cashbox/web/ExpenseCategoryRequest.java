package org.sparkcorporation.lims.cashbox.web;

import org.jspecify.annotations.Nullable;

/**
 * Create/update request. For create: {@code code} and {@code label} required, {@code active}
 * defaults true. For update: {@code code} is ignored (immutable); {@code label} and {@code active}
 * are each optional ({@code null} = no change).
 */
public record ExpenseCategoryRequest(
        @Nullable String code, @Nullable String label, @Nullable Boolean active) {}
