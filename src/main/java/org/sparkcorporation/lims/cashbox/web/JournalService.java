package org.sparkcorporation.lims.cashbox.web;

import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.InvoiceApi;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRepository;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRowProjection;
import org.sparkcorporation.lims.iam.api.UserApi;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalEntryRepository journalRepo;
    private final InvoiceApi invoiceApi;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final UserApi userApi;

    @Transactional(readOnly = true)
    public Page<JournalEntryRow> findJournal(JournalFilters f, Pageable pageable) {
        Page<JournalEntryRowProjection> rows =
                journalRepo.findJournalWithPaidSoFar(
                        f.cashRegisterId() == null ? null : f.cashRegisterId().value(),
                        f.from(),
                        f.to(),
                        f.operationKind() == null ? null : f.operationKind().name(),
                        pageable);

        var labFileIds =
                rows.getContent().stream()
                        .map(JournalEntryRowProjection::getLabFileId)
                        .filter(Objects::nonNull)
                        .map(LabFileId::new)
                        .collect(Collectors.toUnmodifiableSet());
        var patientIds =
                rows.getContent().stream()
                        .map(JournalEntryRowProjection::getPatientId)
                        .filter(Objects::nonNull)
                        .map(PatientId::new)
                        .collect(Collectors.toUnmodifiableSet());
        var userIds =
                rows.getContent().stream()
                        .map(JournalEntryRowProjection::getUserId)
                        .map(UserId::new)
                        .collect(Collectors.toUnmodifiableSet());

        var labFiles = labFileApi.findAllByIds(labFileIds);
        var patients = patientApi.findAllByIds(patientIds);
        var users = userApi.findAllByIds(userIds);
        var invoices = invoiceApi.findAllByLabFileIds(labFileIds);

        return rows.map(p -> JournalMapper.toRow(p, labFiles, patients, users, invoices));
    }
}
