package org.sparkcorporation.lims.catalog.web;

import java.util.List;

import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.AnalyticalDelay;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.domain.Units;
import org.sparkcorporation.lims.catalog.domain.UpdateAnalysis;

/** Hand-written mapper between {@link Analysis} domain and web DTOs. */
@Component
public class AnalysisMapper {

    public NewAnalysis toCommand(CreateAnalysisRequest req) {
        Units units = req.units() == null ? null : toUnits(req.units());
        AnalyticalDelay delay = req.delay() == null ? null : toDelay(req.delay());
        int labelCount = req.labelCount() == null ? 1 : req.labelCount();
        return new NewAnalysis(
                req.code(),
                req.name(),
                req.kind(),
                req.indiciaryValueB(),
                labelCount,
                new DisciplineId(req.disciplineId()),
                req.matrixId() == null ? null : new MatrixId(req.matrixId()),
                req.recipientId() == null ? null : new RecipientId(req.recipientId()),
                req.resultType(),
                req.interpretationText(),
                req.defaultPortoir(),
                req.defaultConservationDays(),
                units,
                delay);
    }

    public UpdateAnalysis toCommand(UpdateAnalysisRequest req) {
        Units units = req.units() == null ? null : toUnits(req.units());
        AnalyticalDelay delay = req.delay() == null ? null : toDelay(req.delay());
        return new UpdateAnalysis(
                req.name(),
                req.indiciaryValueB(),
                req.labelCount(),
                req.matrixId() == null ? null : new MatrixId(req.matrixId()),
                req.recipientId() == null ? null : new RecipientId(req.recipientId()),
                req.resultType(),
                req.interpretationText(),
                req.defaultPortoir(),
                req.defaultConservationDays(),
                units,
                delay);
    }

    private static Units toUnits(UnitsRequest u) {
        return new Units(
                u.unit1(), u.unit1Decimals(), u.unit2(), u.unit2Decimals(), u.conversionFactor());
    }

    private static AnalyticalDelay toDelay(AnalyticalDelayRequest d) {
        return new AnalyticalDelay(d.days(), d.hours(), d.anteriorityCount());
    }

    /** Convert an {@link AnalysisSummary} (built by {@code DefaultAnalysisApi}) to a response. */
    public AnalysisResponse toResponse(AnalysisSummary s, Analysis a) {
        var disciplineSnap =
                new AnalysisResponse.DisciplineSnapshotResponse(
                        s.discipline().id().value(), s.discipline().code(), s.discipline().name());
        var matrixSnap =
                s.matrix() == null
                        ? null
                        : new AnalysisResponse.MatrixSnapshotResponse(
                                s.matrix().id().value(), s.matrix().code(), s.matrix().name());
        var recipientSnap =
                s.recipient() == null
                        ? null
                        : new AnalysisResponse.RecipientSnapshotResponse(
                                s.recipient().id().value(),
                                s.recipient().code(),
                                s.recipient().name());
        var unitsResp =
                new AnalysisResponse.UnitsResponse(
                        s.units().unit1(),
                        s.units().unit1Decimals(),
                        s.units().unit2(),
                        s.units().unit2Decimals(),
                        s.units().conversionFactor());
        var delayResp =
                new AnalysisResponse.AnalyticalDelayResponse(
                        s.delay().days(), s.delay().hours(), s.delay().anteriorityCount());
        List<ComponentLinkResponse> components =
                s.components().stream()
                        .map(
                                c ->
                                        new ComponentLinkResponse(
                                                c.childId().value(),
                                                c.childCode(),
                                                c.childName(),
                                                c.position()))
                        .toList();
        List<TechniqueLinkResponse> techniques =
                s.techniques().stream()
                        .map(
                                t ->
                                        new TechniqueLinkResponse(
                                                t.id().value(), t.code(), t.name(), t.isDefault()))
                        .toList();
        return new AnalysisResponse(
                s.id().value(),
                s.code(),
                s.name(),
                s.kind(),
                s.indiciaryValueB(),
                s.labelCount(),
                disciplineSnap,
                matrixSnap,
                recipientSnap,
                s.resultType(),
                s.interpretationText(),
                s.defaultPortoir(),
                s.defaultConservationDays(),
                unitsResp,
                delayResp,
                components,
                techniques,
                a.createdAt(),
                a.updatedAt(),
                a.version());
    }
}
