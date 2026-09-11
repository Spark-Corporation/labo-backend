package org.sparkcorporation.lims.cashbox.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Create/update request. For create: {@code code} and {@code label} required, {@code active}
 * defaults true. For update: {@code code} is ignored (immutable); {@code label} and {@code active}
 * are each optional ({@code null} = no change).
 */
@Schema(
        description =
                "Expense category create/update body; field requirements differ by operation (see"
                        + " field docs)")
public record ExpenseCategoryRequest(
        @Schema(
                        description =
                                "Category code, uppercase-normalized. Required on create; ignored"
                                        + " on update (immutable once created).")
                @Nullable String code,
        @Schema(description = "Display label. Required on create; on update, null means no change.")
                @Nullable String label,
        @Schema(
                        description =
                                "Whether the category is active. On create defaults to true if"
                                        + " omitted; on update, null means no change.")
                @Nullable Boolean active) {}
