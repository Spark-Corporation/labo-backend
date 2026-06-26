package org.sparkcorporation.lims.result.infra;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalId;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.result.api.ResultSummary.AntibiogramReadingSummary;
import org.sparkcorporation.lims.result.api.ResultSummary.LineSummary;
import org.sparkcorporation.lims.result.domain.AntibiogramReading;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.result.domain.ResultLine;

/**
 * Static, stateless mapper from {@link Result} entity to {@link ResultSummary}. Extracted from
 * {@link DefaultResultApi} so that {@code result/web} services can map queue pages without coupling
 * to the API layer implementation.
 */
public final class ResultSummaryMapper {

    private ResultSummaryMapper() {
        throw new AssertionError("utility class");
    }

    public static ResultSummary toSummary(Result r) {
        var lines = r.getLines().stream().map(ResultSummaryMapper::toLineSummary).toList();
        var readings =
                r.getAntibiogramReadings().stream()
                        .map(ResultSummaryMapper::toAntibiogramReadingSummary)
                        .toList();
        return new ResultSummary(
                new ResultId(r.getId()),
                new LabFileId(r.getLabFileId()),
                new AnalysisRequestId(r.getAnalysisRequestId()),
                new AnalysisId(r.getAnalysisId()),
                r.getAnalysisCodeSnapshot(),
                r.getState(),
                r.getTechnicalValidatedBy(),
                r.getTechnicalValidatedAt(),
                r.getBiologicalValidatedBy(),
                r.getBiologicalValidatedAt(),
                lines,
                readings,
                r.getCreatedAt(),
                r.getCreatedBy());
    }

    private static LineSummary toLineSummary(ResultLine l) {
        return new LineSummary(
                new ResultLineId(l.getId()),
                new AnalysisId(l.getComponentAnalysisId()),
                l.getComponentCodeSnapshot(),
                l.getPosition(),
                l.getResultType(),
                l.getValueInteger(),
                l.getValueReal(),
                l.getValueText(),
                l.getUnit1Snapshot(),
                l.getUnit1Decimals(),
                l.getUnit2Snapshot(),
                l.getUnit2Decimals(),
                l.getUnit1To2Factor(),
                l.getReferenceIntervalId() == null
                        ? null
                        : new ReferenceIntervalId(l.getReferenceIntervalId()),
                l.isOutOfRange(),
                l.getCommentText());
    }

    private static AntibiogramReadingSummary toAntibiogramReadingSummary(AntibiogramReading ar) {
        return new AntibiogramReadingSummary(
                new AntibiogramReadingId(ar.getId()),
                new AntibioticId(ar.getAntibioticId()),
                ar.getMeasuredDiameter(),
                ar.getInterpretedSensitivity(),
                ar.getPosition());
    }
}
