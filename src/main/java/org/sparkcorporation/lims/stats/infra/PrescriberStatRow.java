package org.sparkcorporation.lims.stats.infra;

/**
 * Native-SQL projection for {@code GET /api/v1/stats/by-prescriber} (spec §5.3). One row per
 * prescriber, carrying the count of dossiers registered in range and attributed to that prescriber.
 * Dossiers with no prescriber ({@code lab_files.prescriber_id IS NULL}) are deliberately excluded
 * by the inner join and appear in no row.
 */
public record PrescriberStatRow(String prescriberId, String code, String name, long dossierCount) {}
