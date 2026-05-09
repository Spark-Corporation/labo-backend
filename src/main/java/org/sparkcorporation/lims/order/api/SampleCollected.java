package org.sparkcorporation.lims.order.api;

import java.time.Instant;

import org.sparkcorporation.lims.catalog.api.AnalysisId;

/** Domain event — fired after a sample is collected on a LabFile. */
public record SampleCollected(
        LabFileId labFileId,
        SampleId sampleId,
        SampleDailyId dailyId,
        SampleType sampleType,
        AnalysisId analysisId,
        Instant occurredAt) {}
