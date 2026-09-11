package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import org.sparkcorporation.lims.order.infra.SampleDestructionQueueProjection;

/**
 * Web-tier row of the destruction-due queue (CDC Onglet « Conservation »). Carries no patient PII —
 * staff locate the physical sample by daily-id + portoir; if a UI later needs the patient, add it
 * as a response field only, never to a log line.
 */
@Schema(
        description =
                "A sample past its computed conservation/destruction deadline that has not yet"
                        + " been recorded as destroyed.")
public record SampleDestructionRow(
        @Schema(description = "Sample id.") String sampleId,
        @Schema(description = "Daily-sequence id used to physically locate the sample.")
                String dailyId,
        @Schema(description = "Code of the analysis this sample was collected for.")
                String analysisCode,
        @Schema(description = "Name of the analysis this sample was collected for.")
                String analysisName,
        @Schema(
                        description =
                                "Rack (\"portoir\") the sample was stored in, snapshotted at "
                                        + "collection time.")
                String portoir,
        @Schema(description = "When the sample's conservation period started.")
                Instant conservationStart,
        @Schema(description = "Computed deadline by which the sample must be destroyed.")
                Instant destructionDueAt,
        @Schema(description = "Number of days past destructionDueAt.") int daysOverdue,
        @Schema(description = "Id of the lab file the sample belongs to.") String labFileId,
        @Schema(description = "Human-readable reference of the lab file the sample belongs to.")
                String labFileReference) {

    static SampleDestructionRow from(SampleDestructionQueueProjection p) {
        return new SampleDestructionRow(
                p.getSampleId(),
                p.getDailyId(),
                p.getAnalysisCode(),
                p.getAnalysisName(),
                p.getPortoir(),
                p.getConservationStart(),
                p.getDestructionDueAt(),
                p.getDaysOverdue(),
                p.getLabFileId(),
                p.getLabFileReference());
    }
}
