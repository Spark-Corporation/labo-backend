package org.sparkcorporation.lims.catalog.web;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One audited sub-aggregate row that changed within a single revision (SP12 config history) — a
 * reference interval, grouped-membership component, or technique link. {@code revtype} is {@code
 * ADD}/{@code MOD}/{@code DEL}; {@code fields} is the row's audited column values at that revision
 * (populated even for {@code DEL} thanks to {@code store_data_at_delete}).
 */
@Schema(
        description =
                "One sub-aggregate row (reference interval, component link, or technique link) "
                        + "that changed within a single revision.")
public record ChildChange(
        @Schema(description = "Id of the changed row.") String id,
        @Schema(description = "Kind of change: ADD, MOD (modified), or DEL (deleted).")
                String revtype,
        @Schema(
                        description =
                                "The row's audited column values at this revision; populated even"
                                        + " for DEL.")
                Map<String, Object> fields) {}
