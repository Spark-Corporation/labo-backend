package org.sparkcorporation.lims.order.web;

import org.sparkcorporation.lims.order.api.LabFileSummary;

/**
 * Hand-written mapper translating order sample summaries → web sample responses. Static-method
 * style mirrors the catalog precedent.
 */
public final class SampleMapper {

    private SampleMapper() {}

    public static SampleResponse toResponse(LabFileSummary.SampleSummary s) {
        return switch (s) {
            case LabFileSummary.InLabSampleSummary il ->
                    new SampleResponse.InLab(
                            il.id().value(),
                            il.dailyId().value(),
                            il.analysisCode(),
                            il.recipientCode(),
                            il.collectedAt(),
                            il.collectedByUserId());
            case LabFileSummary.HomeSampleSummary h ->
                    new SampleResponse.Home(
                            h.id().value(),
                            h.dailyId().value(),
                            h.analysisCode(),
                            h.recipientCode(),
                            h.collectedAt(),
                            h.collectedByUserId(),
                            h.addressLine(),
                            h.distanceKm(),
                            h.windowStart(),
                            h.windowEnd(),
                            h.preleveurCode());
            case LabFileSummary.ExternalReceivedSampleSummary e ->
                    new SampleResponse.ExternalReceived(
                            e.id().value(),
                            e.dailyId().value(),
                            e.analysisCode(),
                            e.recipientCode(),
                            e.collectedAt(),
                            e.collectedByUserId(),
                            e.origin(),
                            e.receivedAt(),
                            e.receivedByUserId());
        };
    }
}
