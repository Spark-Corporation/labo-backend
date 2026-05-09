package org.sparkcorporation.lims.order.web;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.SampleId;
import org.sparkcorporation.lims.order.domain.ExternalReceivedSample;
import org.sparkcorporation.lims.order.domain.HomeSample;
import org.sparkcorporation.lims.order.domain.InLabSample;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.infra.DefaultSampleDailyIdGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;

/**
 * Application service that records samples on an OPEN lab-file. Per spec §5.3.1, the discipline
 * prefix that drives the daily-ID is read from {@code analysis.discipline().prefix()} — a single
 * round-trip via {@link AnalysisApi#findById}, not a separate {@code DisciplineApi} call.
 */
@Service
@RequiredArgsConstructor
public class SampleService {

    private final LabFileRepository fileRepo;
    private final LabFileApi labFileApi;
    private final AnalysisApi analysisApi;
    private final DefaultSampleDailyIdGenerator dailyIdGen;
    private final Clock clock;

    @Transactional
    public SampleResponse recordInLab(
            LabFileId fileId, RecordInLabSampleRequest req, @Nullable String userId) {
        var file = requireActive(fileId);
        var analysis =
                analysisApi
                        .findById(new AnalysisId(req.analysisId()))
                        .orElseThrow(
                                () ->
                                        new OrderInvalidAnalysisReferenceException(
                                                req.analysisId(), "not found"));
        var prefix = analysis.discipline().prefix();
        var dailyId =
                dailyIdGen.next(
                        prefix.prefix(), LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
        var sample =
                InLabSample.create(req.analysisId(), req.recipientId(), req.collectedAt(), userId);
        sample.attachToFile(file, dailyId);
        file.addSample(sample, clock);
        return resolveSampleResponse(fileId, sample.sampleId());
    }

    @Transactional
    public SampleResponse recordHome(
            LabFileId fileId, RecordHomeSampleRequest req, @Nullable String userId) {
        var file = requireActive(fileId);
        var analysis =
                analysisApi
                        .findById(new AnalysisId(req.analysisId()))
                        .orElseThrow(
                                () ->
                                        new OrderInvalidAnalysisReferenceException(
                                                req.analysisId(), "not found"));
        var prefix = analysis.discipline().prefix();
        var dailyId =
                dailyIdGen.next(
                        prefix.prefix(), LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
        var sample =
                HomeSample.create(
                        req.analysisId(),
                        req.recipientId(),
                        req.collectedAt(),
                        userId,
                        req.addressLine(),
                        req.distanceKm(),
                        req.windowStart(),
                        req.windowEnd(),
                        req.preleveurId());
        sample.attachToFile(file, dailyId);
        file.addSample(sample, clock);
        return resolveSampleResponse(fileId, sample.sampleId());
    }

    @Transactional
    public SampleResponse recordExternal(
            LabFileId fileId, RecordExternalSampleRequest req, @Nullable String userId) {
        var file = requireActive(fileId);
        var analysis =
                analysisApi
                        .findById(new AnalysisId(req.analysisId()))
                        .orElseThrow(
                                () ->
                                        new OrderInvalidAnalysisReferenceException(
                                                req.analysisId(), "not found"));
        var prefix = analysis.discipline().prefix();
        var dailyId =
                dailyIdGen.next(
                        prefix.prefix(), LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
        var sample =
                ExternalReceivedSample.create(
                        req.analysisId(),
                        req.recipientId(),
                        req.collectedAt(),
                        req.receivedAt(),
                        req.receivedByUserId(),
                        req.origin());
        sample.attachToFile(file, dailyId);
        file.addSample(sample, clock);
        return resolveSampleResponse(fileId, sample.sampleId());
    }

    private LabFile requireActive(LabFileId fileId) {
        return fileRepo.findActiveById(fileId.value())
                .orElseThrow(() -> new LabFileNotFoundException(fileId));
    }

    /**
     * Re-resolves the sample's {@link SampleResponse} via the cross-module snapshot path so the
     * response surfaces the resolved {@code analysisCode} / {@code recipientCode} fields rather
     * than raw IDs. Mirrors {@code DefaultLabFileApi}'s handling.
     */
    private SampleResponse resolveSampleResponse(LabFileId fileId, SampleId sampleId) {
        var summary = labFileApi.requireById(fileId);
        return summary.samples().stream()
                .filter(s -> s.id().equals(sampleId))
                .findFirst()
                .map(SampleMapper::toResponse)
                .orElseThrow();
    }
}
