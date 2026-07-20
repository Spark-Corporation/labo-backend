package org.sparkcorporation.lims.patient.infra;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.InsuranceSummary;
import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerSummary;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.patient.api.PatientSummary.InsuranceSnapshot;
import org.sparkcorporation.lims.patient.api.PatientSummary.PartnerSnapshot;
import org.sparkcorporation.lims.patient.api.RecipientContact;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PatientNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefaultPatientApi implements PatientApi {

    private final PatientRepository repo;
    private final InsuranceApi insuranceApi;
    private final PartnerApi partnerApi;

    @Override
    public Optional<PatientSummary> findById(PatientId id) {
        return repo.findById(id.value()).map(this::toSummary);
    }

    @Override
    public PatientSummary requireById(PatientId id) {
        return repo.findById(id.value())
                .map(this::toSummary)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    @Override
    public PatientId followMergeChain(PatientId id) {
        return repo.findById(id.value())
                .map(p -> p.isMerged() ? new PatientId(p.mergedIntoId()) : id)
                .orElse(id);
    }

    @Override
    public boolean existsById(PatientId id) {
        // Patient uses @SQLRestriction("deleted_at IS NULL") — findById auto-filters soft-deleted.
        return repo.findById(id.value()).isPresent();
    }

    @Override
    public RecipientContact contactFor(PatientId id) {
        return repo.findById(id.value())
                .map(DefaultPatientApi::toContact)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    private static RecipientContact toContact(Patient p) {
        var contact = p.contact();
        var guardian = p.guardian();
        return new RecipientContact(
                contact.email(), contact.phone(), guardian == null ? null : guardian.phone());
    }

    @Override
    public Map<PatientId, PatientSummary> findAllByIds(Set<PatientId> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var rawIds = ids.stream().map(PatientId::value).collect(Collectors.toSet());
        List<Patient> patients = repo.findAllActiveByIds(rawIds);
        Map<InsuranceId, InsuranceSummary> insMap = resolveInsurances(patients);
        Map<PartnerId, PartnerSummary> partMap = resolvePartners(patients);
        return patients.stream()
                .collect(toUnmodifiableMap(Patient::id, p -> toSummary(p, insMap, partMap)));
    }

    private PatientSummary toSummary(Patient p) {
        InsuranceSnapshot ins =
                p.insuranceId() == null
                        ? null
                        : insuranceApi
                                .findById(new InsuranceId(p.insuranceId()))
                                .map(DefaultPatientApi::toInsuranceSnapshot)
                                .orElse(null);
        PartnerSnapshot part =
                p.partnerId() == null
                        ? null
                        : partnerApi
                                .findById(new PartnerId(p.partnerId()))
                                .map(DefaultPatientApi::toPartnerSnapshot)
                                .orElse(null);
        return buildSummary(p, ins, part);
    }

    private PatientSummary toSummary(
            Patient p,
            Map<InsuranceId, InsuranceSummary> insMap,
            Map<PartnerId, PartnerSummary> partMap) {
        InsuranceSnapshot ins =
                p.insuranceId() == null
                        ? null
                        : Optional.ofNullable(insMap.get(new InsuranceId(p.insuranceId())))
                                .map(DefaultPatientApi::toInsuranceSnapshot)
                                .orElse(null);
        PartnerSnapshot part =
                p.partnerId() == null
                        ? null
                        : Optional.ofNullable(partMap.get(new PartnerId(p.partnerId())))
                                .map(DefaultPatientApi::toPartnerSnapshot)
                                .orElse(null);
        return buildSummary(p, ins, part);
    }

    private static PatientSummary buildSummary(
            Patient p,
            @org.jspecify.annotations.Nullable InsuranceSnapshot ins,
            @org.jspecify.annotations.Nullable PartnerSnapshot part) {
        return new PatientSummary(
                p.id(),
                p.labCode(),
                p.name().lastName(),
                p.name().firstNames(),
                p.civility(),
                p.sex(),
                p.birthDate(),
                ins,
                part,
                p.isMerged(),
                p.isDeleted());
    }

    private static InsuranceSnapshot toInsuranceSnapshot(InsuranceSummary s) {
        return new InsuranceSnapshot(s.id(), s.code(), s.name());
    }

    private static PartnerSnapshot toPartnerSnapshot(PartnerSummary s) {
        return new PartnerSnapshot(s.id(), s.code(), s.name(), s.kind());
    }

    private Map<InsuranceId, InsuranceSummary> resolveInsurances(List<Patient> patients) {
        Set<InsuranceId> ids = new HashSet<>();
        for (Patient p : patients) {
            if (p.insuranceId() != null) {
                ids.add(new InsuranceId(p.insuranceId()));
            }
        }
        return ids.isEmpty() ? Map.of() : insuranceApi.findAllByIds(ids);
    }

    private Map<PartnerId, PartnerSummary> resolvePartners(List<Patient> patients) {
        Set<PartnerId> ids = new HashSet<>();
        for (Patient p : patients) {
            if (p.partnerId() != null) {
                ids.add(new PartnerId(p.partnerId()));
            }
        }
        return ids.isEmpty() ? Map.of() : partnerApi.findAllByIds(ids);
    }
}
