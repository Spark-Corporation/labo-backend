package org.sparkcorporation.lims.stats.infra;

/**
 * Native-SQL projection for {@code GET /api/v1/stats/antibiograms} (spec §5.5). One row per
 * antibiotic, carrying the S/I/R bucket counts read from {@code
 * antibiogram_readings.interpreted_sensitivity} over readings whose owning result was registered in
 * range (spec §6.5).
 */
public record AntibiogramStatRow(
        String antibioticId,
        String code,
        String name,
        long sensible,
        long intermediate,
        long resistant) {}
