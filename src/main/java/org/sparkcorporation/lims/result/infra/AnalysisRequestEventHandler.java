package org.sparkcorporation.lims.result.infra;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.order.api.AnalysisRequestAdded;
import org.sparkcorporation.lims.order.api.AnalysisRequestRemoved;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.result.api.ResultCreated;
import org.sparkcorporation.lims.result.api.ResultId;
import org.sparkcorporation.lims.result.api.ResultState;
import org.sparkcorporation.lims.result.domain.NewResult;
import org.sparkcorporation.lims.result.domain.Result;
import org.sparkcorporation.lims.shared.domain.Age;
import org.sparkcorporation.lims.shared.domain.AgeUnit;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sex;

/**
 * Cross-module event subscriber that materializes / removes {@link Result} aggregates in lock-step
 * with {@code order::AnalysisRequest} events per spec §7.1.
 *
 * <p><b>Phase = AFTER_COMMIT.</b> The result aggregate is created only after the order's
 * transaction commits — so a rollback on the order side cannot leave an orphan {@code results} row.
 * The original transaction is already committed/closed when the listener fires, so a fresh one is
 * needed to persist the new {@code Result}; Spring rejects {@code @TransactionalEventListener} +
 * default {@code @Transactional} (would re-enlist into a closed tx) and requires the explicit
 * {@code REQUIRES_NEW} propagation per {@code RestrictedTransactionalEventListenerFactory}.
 *
 * <p><b>Patient category resolution.</b> {@link PatientCategory} requires both {@link Sex sex} and
 * {@link Age age}. The age is derived from {@code patient.birthDate()} (added in chunk 2 per spec
 * §10.5) against {@code labFile.createdAt()}. When the birth date is unknown, the handler defaults
 * to the sex-derived category ({@link PatientCategory#HOMME} for {@link Sex#M}, {@link
 * PatientCategory#FEMME} for {@link Sex#F}) so the snapshot row stays internally consistent with
 * {@code patient_sex_snapshot}; the reference-interval matcher will still return no-match because
 * the age-window check fails on a null {@code ageInDays}, and chunk 5's value-entry path can
 * re-resolve the category once a birth date becomes available.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalysisRequestEventHandler {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private final ResultRepository repo;
    private final AnalysisApi analysisApi;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAnalysisRequestAdded(AnalysisRequestAdded ev) {
        var analysis =
                analysisApi
                        .findByCode(new AnalysisCode(ev.analysisCode()))
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Analysis with code "
                                                        + ev.analysisCode()
                                                        + " not found at result-creation time"));
        var labFile = labFileApi.requireById(ev.labFileId());
        var patient = patientApi.requireById(new PatientId(labFile.patient().id()));

        Integer ageInDays = computeAgeInDays(patient.birthDate(), labFile.createdAt());
        PatientCategory category =
                ageInDays == null
                        ? (patient.sex() == Sex.M ? PatientCategory.HOMME : PatientCategory.FEMME)
                        : PatientCategory.of(patient.sex(), new Age(ageInDays, AgeUnit.DAYS));

        var cmd =
                new NewResult(
                        ev.labFileId(),
                        ev.analysisRequestId(),
                        analysis.id(),
                        analysis.code(),
                        analysis.kind(),
                        analysis.resultType(),
                        analysis.components(),
                        patient.sex(),
                        ageInDays,
                        category,
                        analysis.units().unit1(),
                        analysis.units().unit1Decimals(),
                        analysis.units().unit2(),
                        analysis.units().unit2Decimals());

        Result r = Result.create(cmd, clock);
        repo.save(r);

        events.publishEvent(
                new ResultCreated(
                        new ResultId(r.getId()),
                        ev.labFileId(),
                        ev.analysisRequestId(),
                        analysis.id(),
                        clock.instant()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAnalysisRequestRemoved(AnalysisRequestRemoved ev) {
        repo.findActiveByAnalysisRequestId(ev.analysisRequestId().value())
                .ifPresent(
                        r -> {
                            if (r.getState() == ResultState.DRAFT) {
                                repo.delete(r); // @SQLDelete sets deleted_at
                            } else {
                                log.warn(
                                        "AnalysisRequest {} removed but its Result is in state {};"
                                                + " row preserved as audit trail",
                                        ev.analysisRequestId().value(),
                                        r.getState());
                            }
                        });
    }

    private static @Nullable Integer computeAgeInDays(
            @Nullable LocalDate birthDate, Instant labFileCreatedAt) {
        if (birthDate == null) {
            return null;
        }
        LocalDate asOf = labFileCreatedAt.atZone(UTC).toLocalDate();
        return Math.toIntExact(ChronoUnit.DAYS.between(birthDate, asOf));
    }
}
