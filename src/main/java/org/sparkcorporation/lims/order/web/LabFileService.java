package org.sparkcorporation.lims.order.web;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.AnalysisApi;
import org.sparkcorporation.lims.catalog.api.AnalysisCode;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PreleveurApi;
import org.sparkcorporation.lims.catalog.api.PreleveurId;
import org.sparkcorporation.lims.catalog.api.PrescriberApi;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.order.api.AnalysisRequestAdded;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.domain.DuplicateAnalysisRequestException;
import org.sparkcorporation.lims.order.domain.LabFile;
import org.sparkcorporation.lims.order.domain.LabFileNotFoundException;
import org.sparkcorporation.lims.order.domain.NewAnalysisRequest;
import org.sparkcorporation.lims.order.domain.NewLabFile;
import org.sparkcorporation.lims.order.domain.OrderInvalidAnalysisReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidInsuranceReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPartnerReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPreleveurReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPrescriberReferenceException;
import org.sparkcorporation.lims.order.domain.UpdateLabFile;
import org.sparkcorporation.lims.order.infra.DefaultLabFileRegistrationGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;

/**
 * Application service for {@link LabFile}. Implements spec §7.2 cross-module validation order:
 * patient → prescriber → préleveur → insurance → partner → analyses (resolve + dedup) → counter
 * bump → factory → persist.
 *
 * <p>The counter is bumped <em>only after all validation passes</em> so a failure does not burn a
 * sequence number.
 *
 * <p>1.6.a: when the request omits {@code insuranceId} / {@code partnerId} (or sends {@code null}),
 * the service defaults the FK from the patient's own insurance/partner (a known partial — the
 * "force-clear" intent collides with "default from patient" in this shape).
 */
@Service
@RequiredArgsConstructor
public class LabFileService {

    private final LabFileRepository fileRepo;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final PrescriberApi prescriberApi;
    private final PreleveurApi preleveurApi;
    private final InsuranceApi insuranceApi;
    private final PartnerApi partnerApi;
    private final AnalysisApi analysisApi;
    private final DefaultLabFileRegistrationGenerator regGen;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public LabFileResponse create(CreateLabFileRequest req, @Nullable String userId) {
        var patient = resolvePatient(req);
        validateOptionalReferences(req);
        var resolved = resolveAnalyses(req);
        var rn = regGen.next(LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
        var cmd = buildCommand(req, patient);
        var file = LabFile.open(cmd, rn, userId, clock);
        for (var a : resolved) {
            file.addAnalysisRequest(new NewAnalysisRequest(a.id().value(), a.code()), clock);
        }
        fileRepo.save(file);
        publishAnalysisRequestAdded(file);
        return LabFileMapper.toResponse(labFileApi.requireById(file.labFileId()));
    }

    private PatientSummary resolvePatient(CreateLabFileRequest req) {
        return patientApi
                .findById(new PatientId(req.patientId()))
                .orElseThrow(() -> new OrderInvalidPatientReferenceException(req.patientId()));
    }

    private void validateOptionalReferences(CreateLabFileRequest req) {
        if (req.prescriberId() != null
                && !prescriberApi.existsById(new PrescriberId(req.prescriberId()))) {
            throw new OrderInvalidPrescriberReferenceException(req.prescriberId());
        }
        if (req.preleveurId() != null
                && !preleveurApi.existsById(new PreleveurId(req.preleveurId()))) {
            throw new OrderInvalidPreleveurReferenceException(req.preleveurId());
        }
        if (req.insuranceId() != null && !insuranceApi.exists(new InsuranceId(req.insuranceId()))) {
            throw new OrderInvalidInsuranceReferenceException(req.insuranceId());
        }
        if (req.partnerId() != null && !partnerApi.exists(new PartnerId(req.partnerId()))) {
            throw new OrderInvalidPartnerReferenceException(req.partnerId());
        }
    }

    private List<AnalysisSummary> resolveAnalyses(CreateLabFileRequest req) {
        var seen = new HashSet<String>();
        var resolved = new ArrayList<AnalysisSummary>();
        for (var code : req.analyses()) {
            if (!seen.add(code)) {
                throw new DuplicateAnalysisRequestException(code);
            }
            var summary =
                    analysisApi
                            .findByCode(new AnalysisCode(code))
                            .orElseThrow(
                                    () ->
                                            new OrderInvalidAnalysisReferenceException(
                                                    code, "not found"));
            resolved.add(summary);
        }
        return resolved;
    }

    private NewLabFile buildCommand(CreateLabFileRequest req, PatientSummary patient) {
        return new NewLabFile(
                req.patientId(),
                req.prescriberId(),
                req.preleveurId(),
                req.urgent(),
                req.analyses(),
                resolveInsuranceId(req, patient),
                resolvePartnerId(req, patient),
                req.homeCollectionFee() != null ? req.homeCollectionFee() : BigDecimal.ZERO,
                req.additionalFees() != null ? req.additionalFees() : BigDecimal.ZERO,
                req.discountKind(),
                req.discountValue());
    }

    private void publishAnalysisRequestAdded(LabFile file) {
        var occurredAt = clock.instant();
        for (var ar : file.getAnalyses()) {
            events.publishEvent(
                    new AnalysisRequestAdded(
                            file.labFileId(),
                            ar.analysisRequestId(),
                            ar.getAnalysisCode(),
                            occurredAt));
        }
    }

    private static @Nullable String resolveInsuranceId(
            CreateLabFileRequest req, PatientSummary patient) {
        if (req.insuranceId() != null) {
            return req.insuranceId();
        }
        return patient.insurance() == null ? null : patient.insurance().id().value();
    }

    private static @Nullable String resolvePartnerId(
            CreateLabFileRequest req, PatientSummary patient) {
        if (req.partnerId() != null) {
            return req.partnerId();
        }
        return patient.partner() == null ? null : patient.partner().id().value();
    }

    @Transactional(readOnly = true)
    public LabFileResponse get(LabFileId id) {
        return LabFileMapper.toResponse(labFileApi.requireById(id));
    }

    @Transactional(readOnly = true)
    public LabFileSearchResponse search(
            @Nullable String patientId,
            @Nullable String prescriberId,
            @Nullable Boolean urgent,
            @Nullable LabFileStatus status,
            @Nullable Instant from,
            @Nullable Instant to,
            int page,
            int size) {
        var pageable = PageRequest.of(page, size);
        Page<LabFile> results =
                fileRepo.search(patientId, prescriberId, urgent, status, from, to, pageable);
        List<LabFileResponse> content =
                results.getContent().stream()
                        .map(lf -> LabFileMapper.toResponse(labFileApi.requireById(lf.labFileId())))
                        .toList();
        return new LabFileSearchResponse(
                content,
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    @Transactional
    public LabFileResponse update(LabFileId id, UpdateLabFile cmd) {
        if (cmd.insuranceId() != null && !insuranceApi.exists(new InsuranceId(cmd.insuranceId()))) {
            throw new OrderInvalidInsuranceReferenceException(cmd.insuranceId());
        }
        if (cmd.partnerId() != null && !partnerApi.exists(new PartnerId(cmd.partnerId()))) {
            throw new OrderInvalidPartnerReferenceException(cmd.partnerId());
        }
        var file =
                fileRepo.findActiveById(id.value())
                        .orElseThrow(() -> new LabFileNotFoundException(id));
        file.update(cmd, clock);
        return LabFileMapper.toResponse(labFileApi.requireById(id));
    }

    /**
     * Idempotent soft-delete: when the file is already inactive (or absent), short-circuit with no
     * exception. Mirrors the catalog precedent.
     */
    @Transactional
    public void delete(LabFileId id) {
        var file = fileRepo.findActiveById(id.value()).orElse(null);
        if (file == null) {
            return; // idempotent — already deleted (or never existed)
        }
        fileRepo.delete(file);
    }
}
