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
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeNotFoundException;
import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeRecord;
import org.sparkcorporation.lims.cashbox.infra.HomeCollectionFeeRepository;
import org.sparkcorporation.lims.iam.api.UserId;

/**
 * Spec §5 "Cashbox": uploads the receipt scan AND points {@code receipt_scan_uri} at it,
 * atomically. Both writes hit the same datasource; the blob write is non-transactional and an
 * orphan after rollback is accepted.
 */
@Service
@RequiredArgsConstructor
public class HomeCollectionFeeAttachmentService {

    private final HomeCollectionFeeRepository feeRepo;
    private final AttachmentApi attachmentApi;
    private final Clock clock;

    @Transactional
    public AttachmentSummary uploadAndLink(String feeId, UploadedScan scan, UserId by) {
        var fee = requireFee(feeId);
        var summary =
                attachmentApi.upload(
                        new AttachmentUpload(
                                AttachableType.HOME_COLLECTION_FEE,
                                feeId,
                                scan.filename(),
                                scan.declaredContentType(),
                                scan.bytes(),
                                by.value()));
        fee.attachReceiptScan(summary.storageUri(), by, Instant.now(clock));
        feeRepo.save(fee);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<AttachmentSummary> list(String feeId) {
        requireFee(feeId);
        return attachmentApi.list(AttachableType.HOME_COLLECTION_FEE, feeId);
    }

    @Transactional(readOnly = true)
    public RenderedAttachment download(String feeId, AttachmentId attachmentId) {
        requireFee(feeId);
        return attachmentApi.get(attachmentId);
    }

    @Transactional
    public void softDelete(String feeId, AttachmentId attachmentId) {
        requireFee(feeId);
        attachmentApi.softDelete(attachmentId);
    }

    private HomeCollectionFeeRecord requireFee(String feeId) {
        return feeRepo.findById(feeId)
                .orElseThrow(() -> new HomeCollectionFeeNotFoundException(feeId));
    }
}
