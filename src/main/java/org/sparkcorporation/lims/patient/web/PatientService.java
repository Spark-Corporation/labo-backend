package org.sparkcorporation.lims.patient.web;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.patient.api.PatientCreated;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientMerged;
import org.sparkcorporation.lims.patient.domain.LabCodeGenerator;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PatientMerger;
import org.sparkcorporation.lims.patient.domain.PatientNotFoundException;
import org.sparkcorporation.lims.patient.infra.PatientRepository;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository repo;
    private final LabCodeGenerator generator;
    private final PatientMapper mapper;
    private final PatientMerger merger;
    private final ApplicationEventPublisher events;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional
    public PatientResponse create(CreatePatientRequest req) {
        String hash =
                req.portalPassword() == null ? null : passwordEncoder.encode(req.portalPassword());
        var code = generator.next(LocalDate.now(clock));
        var patient = Patient.create(mapper.toCreateCommand(req, hash), code);
        repo.save(patient);
        events.publishEvent(new PatientCreated(patient.id(), patient.labCode(), clock.instant()));
        return mapper.toResponse(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse get(PatientId id) {
        return repo.findById(id.value())
                .map(mapper::toResponse)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    @Transactional
    public PatientResponse update(PatientId id, UpdatePatientRequest req) {
        var p = repo.findById(id.value()).orElseThrow(() -> new PatientNotFoundException(id));
        var cmd = mapper.toUpdateCommand(req, () -> passwordEncoder.encode(req.portalPassword()));
        p.update(cmd);
        return mapper.toResponse(p);
    }

    @Transactional
    public void delete(PatientId id) {
        var p = repo.findById(id.value()).orElseThrow(() -> new PatientNotFoundException(id));
        repo.delete(p);
    }

    @Transactional
    public PatientResponse merge(PatientId source, PatientId target) {
        var src =
                repo.findById(source.value())
                        .orElseThrow(() -> new PatientNotFoundException(source));
        var tgt =
                repo.findById(target.value())
                        .orElseThrow(() -> new PatientNotFoundException(target));
        merger.validateMerge(src, target, tgt);
        src.mergeInto(target);
        events.publishEvent(new PatientMerged(source, target, clock.instant()));
        return mapper.toResponse(src);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> search(
            String name,
            String phone,
            String idDocumentNumber,
            String labCode,
            int page,
            int size) {
        Page<Patient> result =
                repo.search(
                        emptyToNull(name),
                        emptyToNull(phone),
                        emptyToNull(idDocumentNumber),
                        emptyToNull(labCode),
                        PageRequest.of(page, size));
        return result.map(mapper::toResponse);
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
