package org.sparkcorporation.lims.order.infra;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.InsuranceSummary;
import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerSummary;
import org.sparkcorporation.lims.catalog.api.PreleveurApi;
import org.sparkcorporation.lims.catalog.api.PreleveurId;
import org.sparkcorporation.lims.catalog.api.PreleveurSummary;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.catalog.api.PrescriberSummary;
import org.sparkcorporation.lims.catalog.api.RecipientApi;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.api.RecipientSummary;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.AnalysisRequestSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.BillingBlock;
import org.sparkcorporation.lims.order.api.LabFileSummary.ExternalReceivedSampleSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.HomeSampleSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.InLabSampleSummary;
import org.sparkcorporation.lims.order.api.LabFileSummary.InsuranceSnapshot;
import org.sparkcorporation.lims.order.api.LabFileSummary.PartnerSnapshot;
import org.sparkcorporation.lims.order.api.LabFileSummary.PatientSnapshot;
import org.sparkcorporation.lims.order.api.LabFileSummary.PreleveurSnapshot;
import org.sparkcorporation.lims.order.api.LabFileSummary.PrescriberSnapshot;
import org.sparkcorporation.lims.order.api.LabFileSummary.SampleSummary;
import org.sparkcorporation.lims.order.domain.ExternalReceivedSample;
import org.sparkcorporation.lims.order.domain.HomeSample;
import org.sparkcorporation.lims.order.domain.InLabSample;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.Sample;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;

/**
 * Default {@link LabFileApi} implementation. Read-only — write paths live in {@code LabFileService}
 * (Chunk 5).
 *
 * <p><b>Snapshot resolution.</b> {@link LabFileRepository#findActiveById} returns the lab-file row
 * only (single-row, no JOIN FETCH on collections — see {@link LabFileRepository} class-doc on the
 * multi-bag-fetch split). {@link #toSummary(LabFile)} performs three round-trips: (1) the lab-file
 * row already in hand, (2) {@link SampleRepository#findByLabFileId(String)} for the {@code samples}
 * bag, (3) {@link AnalysisRequestRepository#findByLabFileId(String)} for the {@code analyses} bag.
 * Patient / Prescriber / Préleveur / Insurance / Partner snapshots are then resolved via the
 * cross-module {@code *Api.findById} reads.
 *
 * <p><b>Soft-deleted parents render gracefully.</b> Per spec §6.2, when a Patient / Prescriber /
 * Préleveur / Insurance / Partner referenced by an active LabFile has been soft-deleted, the
 * corresponding {@code *Api.findById} returns {@link Optional#empty()} (catalog filters {@code
 * deleted_at IS NULL}). Snapshot resolution uses {@link Optional#orElse(Object)} (not {@code
 * orElseThrow}) for the nullable fields so the dossier still renders. The Patient FK is NOT NULL —
 * a missing patient row indicates a Liquibase-invariant violation and surfaces as {@link
 * java.util.NoSuchElementException}.
 *
 * <p><b>Catalog code resolution.</b> {@code analysisCode} / {@code recipientCode} for samples
 * resolve via the catalog APIs and fall back to {@code "?"} when the dictionary entry has been
 * soft-deleted — the analysis-request snapshot already preserves the historical code at request
 * time so no fallback is needed there.
 *
 * <p><b>Billing block.</b> Always non-null; fees coerce to {@link BigDecimal#ZERO} if the entity
 * somehow stores a null. Discount pair passes through as stored.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultLabFileApi implements LabFileApi {

    private final LabFileRepository fileRepo;
    private final SampleRepository sampleRepo;
    private final AnalysisRequestRepository requestRepo;
    private final PatientApi patientApi;
    private final PrescriberApi prescriberApi;
    private final PreleveurApi preleveurApi;
    private final InsuranceApi insuranceApi;
    private final PartnerApi partnerApi;
    private final AnalysisApi analysisApi;
    private final RecipientApi recipientApi;

    @Override
    public Optional<LabFileSummary> findById(LabFileId id) {
        return fileRepo.findActiveById(id.value()).map(this::toSummary);
    }

    @Override
    public Optional<LabFileSummary> findByRegistrationNumber(LabFileRegistrationNumber rn) {
        return fileRepo.findActiveByRegistrationNumber(rn.value()).map(this::toSummary);
    }

    @Override
    public LabFileSummary requireById(LabFileId id) {
        return fileRepo.findActiveById(id.value())
                .map(this::toSummary)
                .orElseThrow(() -> new LabFileNotFoundException(id));
    }

    @Override
    public boolean existsById(LabFileId id) {
        return fileRepo.findActiveById(id.value()).isPresent();
    }

    @Override
    public List<LabFileSummary> findActiveByPatient(String patientId) {
        return fileRepo.findActiveByPatientId(patientId).stream().map(this::toSummary).toList();
    }

    @Override
    public Map<LabFileId, LabFileSummary> findAllByIds(Set<LabFileId> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var rawIds = ids.stream().map(LabFileId::value).collect(Collectors.toSet());
        return fileRepo.findAllActiveByIds(rawIds).stream()
                .collect(toUnmodifiableMap(lf -> lf.labFileId(), this::toSummary));
    }

    private LabFileSummary toSummary(LabFile lf) {
        var samples =
                sampleRepo.findByLabFileId(lf.getId()).stream().map(this::toSampleSummary).toList();
        var analyses =
                requestRepo.findByLabFileId(lf.getId()).stream()
                        .map(
                                ar ->
                                        new AnalysisRequestSummary(
                                                new AnalysisRequestId(ar.getId()),
                                                ar.getAnalysisId(),
                                                ar.getAnalysisCode(),
                                                ar.getRequestedAt()))
                        .toList();

        var patient =
                patientApi
                        .findById(new PatientId(lf.getPatientId()))
                        .map(DefaultLabFileApi::toPatientSnapshot)
                        .orElseThrow();

        var prescriber =
                lf.getPrescriberId() == null
                        ? null
                        : prescriberApi
                                .findById(new PrescriberId(lf.getPrescriberId()))
                                .map(DefaultLabFileApi::toPrescriberSnapshot)
                                .orElse(null);

        var preleveur =
                lf.getPreleveurId() == null
                        ? null
                        : preleveurApi
                                .findById(new PreleveurId(lf.getPreleveurId()))
                                .map(DefaultLabFileApi::toPreleveurSnapshot)
                                .orElse(null);

        var insurance =
                lf.getInsuranceId() == null
                        ? null
                        : insuranceApi
                                .findById(new InsuranceId(lf.getInsuranceId()))
                                .map(DefaultLabFileApi::toInsuranceSnapshot)
                                .orElse(null);

        var partner =
                lf.getPartnerId() == null
                        ? null
                        : partnerApi
                                .findById(new PartnerId(lf.getPartnerId()))
                                .map(DefaultLabFileApi::toPartnerSnapshot)
                                .orElse(null);

        var homeFee =
                lf.getHomeCollectionFee() == null ? BigDecimal.ZERO : lf.getHomeCollectionFee();
        var additionalFees =
                lf.getAdditionalFees() == null ? BigDecimal.ZERO : lf.getAdditionalFees();
        var billing =
                new BillingBlock(
                        homeFee, additionalFees, lf.getDiscountKind(), lf.getDiscountValue());

        return new LabFileSummary(
                lf.labFileId(),
                lf.getRegistrationNumber(),
                patient,
                prescriber,
                preleveur,
                lf.isUrgent(),
                lf.getStatus(),
                samples,
                analyses,
                insurance,
                partner,
                billing,
                lf.getCreatedAt(),
                lf.getCreatedByUserId(),
                lf.getSignedBy(),
                lf.getSignedAt());
    }

    private SampleSummary toSampleSummary(Sample s) {
        var analysisCode =
                analysisApi
                        .findById(new AnalysisId(s.getAnalysisId()))
                        .map(AnalysisSummary::code)
                        .orElse("?");
        var recipientCode =
                recipientApi
                        .findById(new RecipientId(s.getRecipientId()))
                        .map(RecipientSummary::code)
                        .orElse("?");
        return switch (s) {
            case InLabSample il ->
                    new InLabSampleSummary(
                            il.sampleId(),
                            il.getDailyId(),
                            analysisCode,
                            recipientCode,
                            il.getCollectedAt(),
                            il.getCollectedByUserId());
            case HomeSample h ->
                    new HomeSampleSummary(
                            h.sampleId(),
                            h.getDailyId(),
                            analysisCode,
                            recipientCode,
                            h.getCollectedAt(),
                            h.getCollectedByUserId(),
                            h.getAddressLine(),
                            h.getDistanceKm(),
                            h.getWindowStart(),
                            h.getWindowEnd(),
                            preleveurApi
                                    .findById(new PreleveurId(h.getHomePreleveurId()))
                                    .map(PreleveurSummary::code)
                                    .orElse("?"));
            case ExternalReceivedSample e ->
                    new ExternalReceivedSampleSummary(
                            e.sampleId(),
                            e.getDailyId(),
                            analysisCode,
                            recipientCode,
                            e.getCollectedAt(),
                            e.getCollectedByUserId(),
                            e.getOrigin(),
                            e.getReceivedAt(),
                            e.getReceivedByUserId());
            default -> throw new IllegalStateException("unknown Sample subtype: " + s.getClass());
        };
    }

    private static PatientSnapshot toPatientSnapshot(PatientSummary p) {
        return new PatientSnapshot(
                p.id().value(), p.labCode(), p.lastName(), p.firstNames(), p.sex());
    }

    private static PrescriberSnapshot toPrescriberSnapshot(PrescriberSummary p) {
        return new PrescriberSnapshot(
                p.id().value(),
                p.code(),
                p.name(),
                p.specialty() == null ? null : p.specialty().code());
    }

    private static PreleveurSnapshot toPreleveurSnapshot(PreleveurSummary p) {
        return new PreleveurSnapshot(p.id().value(), p.code(), p.name());
    }

    private static InsuranceSnapshot toInsuranceSnapshot(InsuranceSummary i) {
        return new InsuranceSnapshot(i.id(), i.code(), i.name());
    }

    private static PartnerSnapshot toPartnerSnapshot(PartnerSummary p) {
        return new PartnerSnapshot(p.id(), p.code(), p.name(), p.kind());
    }
}
