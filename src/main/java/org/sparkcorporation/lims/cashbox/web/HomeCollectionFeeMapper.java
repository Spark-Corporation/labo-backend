package org.sparkcorporation.lims.cashbox.web;

import org.sparkcorporation.lims.cashbox.domain.HomeCollectionFeeRecord;

final class HomeCollectionFeeMapper {
    private HomeCollectionFeeMapper() {}

    static HomeCollectionFeeResponse toResponse(HomeCollectionFeeRecord f) {
        return new HomeCollectionFeeResponse(
                f.getId(),
                f.getPatientId(),
                f.getLabFileId(),
                f.getDistanceKm(),
                f.getAmount(),
                f.getReceiptScanUri(),
                f.getCashRegisterId(),
                f.getRecordedBy(),
                f.getRecordedAt());
    }
}
