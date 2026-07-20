package org.sparkcorporation.lims.cashbox.web;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.attachment.api.AttachableType;
import org.sparkcorporation.lims.attachment.api.AttachmentApi;
import org.sparkcorporation.lims.attachment.api.AttachmentId;
import org.sparkcorporation.lims.attachment.api.AttachmentSummary;
import org.sparkcorporation.lims.attachment.api.AttachmentUpload;
import org.sparkcorporation.lims.attachment.api.RenderedAttachment;
import org.sparkcorporation.lims.cashbox.domain.Expense;
import org.sparkcorporation.lims.cashbox.domain.ExpenseNotFoundException;
import org.sparkcorporation.lims.cashbox.infra.ExpenseRepository;
import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Spec §5 "Cashbox": uploads the justification scan AND points {@code justification_scan_uri} at
 * it, atomically. Both writes hit the same datasource; the blob write is non-transactional and an
 * orphan after rollback is accepted.
 */
@Service
@RequiredArgsConstructor
public class ExpenseAttachmentService {

    private final ExpenseRepository expenseRepo;
    private final AttachmentApi attachmentApi;
    private final Clock clock;

    @Transactional
    public AttachmentSummary uploadAndLink(String expenseId, UploadedScan scan, UserId by) {
        var expense = requireExpense(expenseId);
        var summary =
                attachmentApi.upload(
                        new AttachmentUpload(
                                AttachableType.EXPENSE,
                                expenseId,
                                scan.filename(),
                                scan.declaredContentType(),
                                scan.bytes(),
                                by.value()));
        expense.attachJustificationScan(summary.storageUri(), by, Instant.now(clock));
        expenseRepo.save(expense);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<AttachmentSummary> list(String expenseId) {
        requireExpense(expenseId);
        return attachmentApi.list(AttachableType.EXPENSE, expenseId);
    }

    @Transactional(readOnly = true)
    public RenderedAttachment download(String expenseId, AttachmentId attachmentId) {
        requireExpense(expenseId);
        return attachmentApi.get(attachmentId);
    }

    @Transactional
    public void softDelete(String expenseId, AttachmentId attachmentId) {
        requireExpense(expenseId);
        attachmentApi.softDelete(attachmentId);
    }

    private Expense requireExpense(String expenseId) {
        return expenseRepo
                .findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
    }
}
