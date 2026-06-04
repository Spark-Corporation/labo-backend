package org.sparkcorporation.lims.cashbox.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.CashRegisterId;
import org.sparkcorporation.lims.cashbox.api.HomeCollectionFeeId;
import org.sparkcorporation.lims.iam.api.UserId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.api.PatientId;

@Entity
@Table(name = "cashbox_home_collection_fees")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HomeCollectionFeeRecord {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(name = "patient_id", nullable = false, length = 26)
    private String patientId;

    @Column(name = "lab_file_id", nullable = false, length = 26)
    private String labFileId;

    @Column(name = "distance_km", precision = 8, scale = 2)
    private BigDecimal distanceKm;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "receipt_scan_uri")
    private String receiptScanUri;

    @Column(name = "cash_register_id", nullable = false, length = 26)
    private String cashRegisterId;

    @Column(name = "recorded_by", nullable = false, length = 26)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 26)
    private String createdBy;

    @Column(name = "updated_by", length = 26)
    private String updatedBy;

    @Version private long version;

    public static HomeCollectionFeeRecord record(
            PatientId patientId,
            LabFileId labFileId,
            BigDecimal distanceKm,
            BigDecimal amount,
            @Nullable String receiptScanUri,
            CashRegisterId cashRegisterId,
            UserId recordedBy,
            Clock clock) {
        if (distanceKm == null || distanceKm.signum() < 0) {
            throw new IllegalArgumentException("distanceKm must be >= 0");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        var now = Instant.now(clock);
        var f = new HomeCollectionFeeRecord();
        f.id = HomeCollectionFeeId.generate().value();
        f.patientId = patientId.value();
        f.labFileId = labFileId.value();
        f.distanceKm = distanceKm;
        f.amount = amount;
        f.receiptScanUri = receiptScanUri;
        f.cashRegisterId = cashRegisterId.value();
        f.recordedBy = recordedBy.value();
        f.recordedAt = now;
        f.createdAt = now;
        f.updatedAt = now;
        f.createdBy = recordedBy.value();
        f.updatedBy = recordedBy.value();
        return f;
    }

    public HomeCollectionFeeId homeCollectionFeeId() {
        return new HomeCollectionFeeId(id);
    }

    /**
     * SP6 (spec §5): points this fee record at its uploaded receipt scan. No lab-file editability
     * guard — the scan touches only this row, and §8.6 keeps scan upload independent of recording.
     */
    public void attachReceiptScan(String storageUri, UserId by, Instant now) {
        this.receiptScanUri = storageUri;
        this.updatedAt = now;
        this.updatedBy = by.value();
    }
}
