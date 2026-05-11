package org.sparkcorporation.lims.order.web;

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
import org.sparkcorporation.lims.order.domain.OrderInvalidPatientReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPreleveurReferenceException;
import org.sparkcorporation.lims.order.domain.OrderInvalidPrescriberReferenceException;
import org.sparkcorporation.lims.order.domain.UpdateLabFile;
import org.sparkcorporation.lims.order.infra.DefaultLabFileRegistrationGenerator;
import org.sparkcorporation.lims.order.infra.LabFileRepository;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;

/**
 * Application service for {@link LabFile}. Implements spec §7.2 cross-module validation order:
 * patient → prescriber → préleveur → analyses (resolve + dedup) → counter bump → factory → persist.
 *
 * <p>The counter is bumped <em>only after all validation passes</em> so a failure does not burn a
 * sequence number.
 */
@Service
@RequiredArgsConstructor
public class LabFileService {

    private final LabFileRepository fileRepo;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final PrescriberApi prescriberApi;
    private final PreleveurApi preleveurApi;
    private final AnalysisApi analysisApi;
    private final DefaultLabFileRegistrationGenerator regGen;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public LabFileResponse create(CreateLabFileRequest req, @Nullable String userId) {
        // 1. patient — required
        if (!patientApi.existsById(new PatientId(req.patientId()))) {
            throw new OrderInvalidPatientReferenceException(req.patientId());
        }
        // 2. prescriber — optional
        if (req.prescriberId() != null
                && !prescriberApi.existsById(new PrescriberId(req.prescriberId()))) {
            throw new OrderInvalidPrescriberReferenceException(req.prescriberId());
        }
        // 3. preleveur — optional
        if (req.preleveurId() != null
                && !preleveurApi.existsById(new PreleveurId(req.preleveurId()))) {
            throw new OrderInvalidPreleveurReferenceException(req.preleveurId());
        }
        // 4. analyses — resolve every code + dedup within the request
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
        // 5. counter bump — only after all validation passes (no burned sequence numbers)
        var rn = regGen.next(LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC));
        // 6. domain factory
        var cmd =
                new NewLabFile(
                        req.patientId(),
                        req.prescriberId(),
                        req.preleveurId(),
                        req.urgent(),
                        req.analyses());
        var file = LabFile.open(cmd, rn, userId, clock);
        // 6b. add the analysis requests to the aggregate
        for (var a : resolved) {
            file.addAnalysisRequest(new NewAnalysisRequest(a.id().value(), a.code()), clock);
        }
        // 7. persist + return rich response (re-resolves cross-module snapshots via LabFileApi)
        fileRepo.save(file);
        // 8. publish AnalysisRequestAdded for each newly-attached analysis. The
        //    result module's AnalysisRequestEventHandler subscribes via @TransactionalEventListener
        //    (AFTER_COMMIT) and materializes the matching Result aggregate. Publishing here ensures
        //    Spring's event registry sees the events while the surrounding @Transactional is open;
        //    delivery is deferred until commit so a rollback drops them.
        var occurredAt = clock.instant();
        for (var ar : file.getAnalyses()) {
            events.publishEvent(
                    new AnalysisRequestAdded(
                            file.labFileId(),
                            ar.analysisRequestId(),
                            ar.getAnalysisCode(),
                            occurredAt));
        }
        return LabFileMapper.toResponse(labFileApi.requireById(file.labFileId()));
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
