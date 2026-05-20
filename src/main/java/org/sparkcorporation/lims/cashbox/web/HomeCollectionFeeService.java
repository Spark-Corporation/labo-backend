package org.sparkcorporation.lims.cashbox.web;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.billing.api.CashRegisterApi;
import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeLabFileNotEditableException;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeRecord;
import org.sparkcorporation.lims.cashbox.domain.JournalEntry;
import org.sparkcorporation.lims.cashbox.infra.HomeCollectionFeeRepository;
import org.sparkcorporation.lims.cashbox.infra.JournalEntryRepository;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileApi;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.LabFileWriteApi;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientId;

@Service
@RequiredArgsConstructor
public class HomeCollectionFeeService {

    private final HomeCollectionFeeRepository feeRepo;
    private final JournalEntryRepository journalRepo;
    private final LabFileWriteApi labFileWriteApi;
    private final LabFileApi labFileApi;
    private final PatientApi patientApi;
    private final CashRegisterApi cashRegisterApi;
    private final Clock clock;

    @Transactional
    public HomeCollectionFeeResponse record(HomeCollectionFeeRequest req, UserId by) {
        var patientId = new PatientId(req.patientId());
        var labFileId = new LabFileId(req.labFileId());
        var cashRegisterId = new CashRegisterId(req.cashRegisterId());
        if (!patientApi.existsById(patientId)) {
            throw new IllegalArgumentException("patientId does not exist: " + patientId.value());
        }
        var labFileSummary =
                labFileApi
                        .findById(labFileId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "labFileId does not exist: " + labFileId.value()));
        if (labFileSummary.status() != LabFileStatus.OPEN) {
            throw new HomeCollectionFeeLabFileNotEditableException(labFileId.value(), null);
        }
        if (!cashRegisterApi.existsById(cashRegisterId)) {
            throw new IllegalArgumentException(
                    "cashRegisterId does not exist: " + cashRegisterId.value());
        }
        labFileWriteApi.setHomeCollectionFee(labFileId, req.amount(), by);
        var fee =
                HomeCollectionFeeRecord.record(
                        patientId,
                        labFileId,
                        req.distanceKm(),
                        req.amount(),
                        req.receiptScanUri(),
                        cashRegisterId,
                        by,
                        clock);
        var saved = feeRepo.save(fee);
        var journal =
                JournalEntry.forHomeCollection(
                        saved.getId(),
                        cashRegisterId,
                        by,
                        saved.getLabFileId(),
                        saved.getPatientId(),
                        saved.getAmount(),
                        saved.getRecordedAt());
        journalRepo.insertIdempotent(
                journal.getId(),
                journal.getOperationKind().name(),
                journal.getSourceId(),
                journal.getCashRegisterId(),
                journal.getUserId(),
                journal.getLabFileId(),
                journal.getPatientId(),
                null,
                journal.getAmount(),
                journal.getOccurredAt());
        return HomeCollectionFeeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<HomeCollectionFeeResponse> list(
            HomeCollectionFeeFilters filters, Pageable pageable) {
        return feeRepo.search(
                        filters.from(),
                        filters.to(),
                        filters.labFileId() == null ? null : filters.labFileId().value(),
                        filters.recordedBy() == null ? null : filters.recordedBy().value(),
                        filters.cashRegisterId() == null ? null : filters.cashRegisterId().value(),
                        pageable)
                .map(HomeCollectionFeeMapper::toResponse);
    }
}
