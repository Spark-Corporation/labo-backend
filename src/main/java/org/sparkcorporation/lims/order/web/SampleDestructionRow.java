package org.sparkcorporation.lims.order.web;

import java.time.Instant;

import org.sparkcorporation.lims.order.infra.SampleDestructionQueueProjection;

/**
 * Web-tier row of the destruction-due queue (CDC Onglet « Conservation »). Carries no patient PII —
 * staff locate the physical sample by daily-id + portoir; if a UI later needs the patient, add it
 * as a response field only, never to a log line.
 */
public record SampleDestructionRow(
        String sampleId,
        String dailyId,
        String analysisCode,
        String analysisName,
        String portoir,
        Instant conservationStart,
        Instant destructionDueAt,
        int daysOverdue,
        String labFileId,
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
