package org.sparkcorporation.lims.catalog.web;

import java.util.Map;

/**
 * One audited sub-aggregate row that changed within a single revision (SP12 config history) — a
 * reference interval, grouped-membership component, or technique link. {@code revtype} is {@code
 * ADD}/{@code MOD}/{@code DEL}; {@code fields} is the row's audited column values at that revision
 * (populated even for {@code DEL} thanks to {@code store_data_at_delete}).
 */
public record ChildChange(String id, String revtype, Map<String, Object> fields) {}
