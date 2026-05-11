package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;

import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.result.domain.FormulaResult;
import org.sparkcorporation.lims.result.domain.FreeFormResult;
import org.sparkcorporation.lims.result.domain.IntegerResult;
import org.sparkcorporation.lims.result.domain.NumericResult;
import org.sparkcorporation.lims.result.domain.ResultValue;
import org.sparkcorporation.lims.result.domain.TextResult;

/**
 * Static-only mapper translating between {@link ResultSummary} (cross-module read-shape) and the
 * web-tier {@link ResultResponse}, plus the {@link PatchLineRequest.ValueUnion} → domain {@link
 * ResultValue} switch.
 */
public final class ResultMapper {

    private ResultMapper() {}

    /**
     * DTO → domain. The 5-arm switch is exhaustive over the sealed {@link
     * PatchLineRequest.ValueUnion}.
     */
    public static ResultValue toDomainValue(PatchLineRequest.ValueUnion union) {
        return switch (union) {
            case PatchLineRequest.ValueUnion.IntegerVariant i -> new IntegerResult(i.value());
            case PatchLineRequest.ValueUnion.RealVariant r ->
                    new NumericResult(new BigDecimal(r.value()));
            case PatchLineRequest.ValueUnion.TextVariant t -> new TextResult(t.value());
            case PatchLineRequest.ValueUnion.FormulaVariant f -> new FormulaResult(f.value());
            case PatchLineRequest.ValueUnion.FreeVariant ff -> new FreeFormResult(ff.value());
        };
    }

    public static ResultResponse toResponse(ResultSummary s) {
        return new ResultResponse(
                s.id().value(),
                s.labFileId().value(),
                s.analysisRequestId().value(),
                s.analysisId().value(),
                s.analysisCodeSnapshot(),
                s.state().name(),
                s.technicalValidatedBy(),
                s.technicalValidatedAt(),
                s.biologicalValidatedBy(),
                s.biologicalValidatedAt(),
                s.lines().stream().map(ResultMapper::toLineResponse).toList(),
                s.antibiogramReadings().stream()
                        .map(ResultMapper::toAntibiogramReadingResponse)
                        .toList(),
                s.createdAt(),
                s.createdByUserId());
    }

    public static ResultResponse.LineResponse toLineResponse(ResultSummary.LineSummary l) {
        return new ResultResponse.LineResponse(
                l.id().value(),
                l.componentAnalysisId().value(),
                l.componentCodeSnapshot(),
                l.position(),
                l.resultType().name(),
                l.valueInteger(),
                l.valueReal(),
                l.valueText(),
                l.unit1Snapshot(),
                l.unit1Decimals(),
                l.unit2Snapshot(),
                l.unit2Decimals(),
                l.referenceIntervalId() == null ? null : l.referenceIntervalId().value(),
                l.isOutOfRange(),
                l.commentText());
    }

    public static ResultResponse.AntibiogramReadingResponse toAntibiogramReadingResponse(
            ResultSummary.AntibiogramReadingSummary a) {
        return new ResultResponse.AntibiogramReadingResponse(
                a.id().value(),
                a.antibioticId().value(),
                a.measuredDiameter().toPlainString(),
                a.interpretedSensitivity().name(),
                a.position());
    }
}
