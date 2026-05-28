package org.sparkcorporation.lims.result.web;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AntibioticApi;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.ReferenceIntervalSummary;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.result.api.AntibiogramReadingId;
import org.sparkcorporation.lims.result.api.BiologicalValidationDone;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultEntered;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultLineId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.api.ResultSummary;
import org.sparkcorporation.lims.result.api.TechnicalValidationDone;
import org.sparkcorporation.lims.result.domain.AntibiogramAntibioticNotFoundException;
import org.sparkcorporation.lims.result.domain.DossierLockedException;
import org.sparkcorporation.lims.result.domain.NewAntibiogramReading;
import org.sparkcorporation.lims.result.domain.NewLineValue;
import org.sparkcorporation.lims.result.domain.ReferenceIntervalMatcher;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.result.domain.ResultNotFoundException;
import org.sparkcorporation.lims.result.domain.ResultValue;
import org.sparkcorporation.lims.result.infra.ResultRepository;

/**
 * Application service for {@link Result}. Per spec §6 + §7.3 each write path either persists
 * implicitly via the open transaction (Hibernate dirty-checking) or via {@code repo.delete(...)}
 * for the soft-delete case, then publishes the appropriate cross-module event.
 *
 * <p>State-flip events are published by inspecting the aggregate state before/after the mutation:
 * {@link ResultEntered} when a {@code patchLine} flips DRAFT→ENTERED; {@link
 * TechnicalValidationDone} after {@code :validate-technical}; {@link BiologicalValidationDone}
 * after {@code :validate-biological}. The {@code patchLine} event is needed only on the DRAFT→
 * ENTERED transition; line patches that keep the result in ENTERED do NOT re-publish.
 */
@Service
@RequiredArgsConstructor
public class ResultService {

    private final ResultRepository repo;
    private final ResultApi resultApi;
    private final LabFileApi labFileApi;
    private final AnalysisApi analysisApi;
    private final AntibioticApi antibioticApi;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ResultSummary get(ResultId id) {
        return resultApi.requireById(id);
    }

    @Transactional(readOnly = true)
    public List<ResultSummary> findByLabFile(LabFileId labFileId) {
        return resultApi.findActiveByLabFile(labFileId);
    }

    @Transactional
    public ResultSummary patchLine(ResultId id, ResultLineId lineId, PatchLineRequest req) {
        Result r =
                repo.findActiveById(id.value()).orElseThrow(() -> new ResultNotFoundException(id));
        assertDossierEditable(new LabFileId(r.getLabFileId()));
        ResultValue val = req.value() == null ? null : ResultMapper.toDomainValue(req.value());
        ReferenceIntervalSummary matched =
                val == null ? null : matchInterval(r, new AnalysisId(r.getAnalysisId()));
        boolean wasDraft = r.getState() == ResultState.DRAFT;
        r.setLineValue(lineId, new NewLineValue(val, req.commentText()), matched);
        if (wasDraft && r.getState() == ResultState.ENTERED) {
            events.publishEvent(
                    new ResultEntered(
                            new ResultId(r.getId()),
                            new LabFileId(r.getLabFileId()),
                            clock.instant()));
        }
        return resultApi.requireById(id);
    }

    @Transactional
    public void clearLineValue(ResultId id, ResultLineId lineId) {
        Result r =
                repo.findActiveById(id.value()).orElseThrow(() -> new ResultNotFoundException(id));
        assertDossierEditable(new LabFileId(r.getLabFileId()));
        r.clearLineValue(lineId);
    }

    @Transactional
    public ResultSummary.AntibiogramReadingSummary addAntibiogramReading(
            ResultId id, AddAntibiogramReadingRequest req) {
        Result r =
                repo.findActiveById(id.value()).orElseThrow(() -> new ResultNotFoundException(id));
        assertDossierEditable(new LabFileId(r.getLabFileId()));
        var ab = new AntibioticId(req.antibioticId());
        var summary =
                antibioticApi
                        .findById(ab)
                        .orElseThrow(() -> new AntibiogramAntibioticNotFoundException(ab));
        var reading =
                r.addAntibiogramReading(
                        new NewAntibiogramReading(
                                ab, new BigDecimal(req.measuredDiameter()), summary));
        return new ResultSummary.AntibiogramReadingSummary(
                new AntibiogramReadingId(reading.getId()),
                new AntibioticId(reading.getAntibioticId()),
                reading.getMeasuredDiameter(),
                reading.getInterpretedSensitivity(),
                reading.getPosition());
    }

    @Transactional
    public void removeAntibiogramReading(ResultId id, AntibiogramReadingId readingId) {
        Result r =
                repo.findActiveById(id.value()).orElseThrow(() -> new ResultNotFoundException(id));
        assertDossierEditable(new LabFileId(r.getLabFileId()));
        r.removeAntibiogramReading(readingId);
    }

    @Transactional
    public ResultSummary validateTechnical(ResultId id, String userId) {
        Result r =
                repo.findActiveById(id.value()).orElseThrow(() -> new ResultNotFoundException(id));
        assertDossierEditable(new LabFileId(r.getLabFileId()));
        r.validateTechnical(userId, clock);
        events.publishEvent(
                new TechnicalValidationDone(
                        new ResultId(r.getId()),
                        new LabFileId(r.getLabFileId()),
                        userId,
                        clock.instant()));
        return resultApi.requireById(id);
    }

    @Transactional
    public ResultSummary validateBiological(ResultId id, String userId) {
        Result r =
                repo.findActiveById(id.value()).orElseThrow(() -> new ResultNotFoundException(id));
        assertDossierEditable(new LabFileId(r.getLabFileId()));
        r.validateBiological(userId, clock);
        events.publishEvent(
                new BiologicalValidationDone(
                        new ResultId(r.getId()),
                        new LabFileId(r.getLabFileId()),
                        new AnalysisRequestId(r.getAnalysisRequestId()),
                        userId,
                        clock.instant()));
        return resultApi.requireById(id);
    }

    @Transactional
    public void delete(ResultId id) {
        Result r =
                repo.findActiveById(id.value()).orElseThrow(() -> new ResultNotFoundException(id));
        assertDossierEditable(new LabFileId(r.getLabFileId()));
        r.delete(); // throws ResultNotDeletableInStateException if not DRAFT
        repo.delete(r); // @SQLDelete sets deleted_at
    }

    /**
     * Refuses any result mutation once the owning dossier is electronically {@code SIGNED} (or
     * already {@code PUBLISHED}). The signed content is cryptographically attested (SP3), so the
     * dossier-level lock supersedes the result aggregate's own state machine.
     */
    private void assertDossierEditable(LabFileId labFileId) {
        var status = labFileApi.requireById(labFileId).status();
        if (status == LabFileStatus.SIGNED || status == LabFileStatus.PUBLISHED) {
            throw new DossierLockedException(labFileId);
        }
    }

    private @Nullable ReferenceIntervalSummary matchInterval(Result r, AnalysisId analysisId) {
        var intervals = analysisApi.findActiveReferenceIntervalsByAnalysis(analysisId);
        return ReferenceIntervalMatcher.match(
                        r.getPatientCategorySnapshot(),
                        r.getPatientSexSnapshot(),
                        r.getPatientAgeInDaysSnapshot(),
                        intervals)
                .orElse(null);
    }
}
