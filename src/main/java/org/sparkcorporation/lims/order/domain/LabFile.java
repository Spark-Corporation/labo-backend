package org.sparkcorporation.lims.order.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.DiscountKind;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Lab file (dossier) aggregate root — soft-deleted via Hibernate {@link SQLDelete}, with explicit
 * version-aware UPDATE to preserve optimistic-lock semantics on concurrent deletes.
 *
 * <p>Per spec §4.4: deliberately NO {@code @SQLRestriction} so cross-module FK integrity from
 * historical lab files is preserved; repositories explicitly filter {@code deleted_at IS NULL}.
 *
 * <p>Mutators reject any operation when {@code status != OPEN}, raising {@link
 * LabFileNotEditableException}.
 */
@Entity
@Table(name = "lab_files")
@SQLDelete(
        sql =
                "UPDATE lab_files SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LabFile {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "registration_number"))
    private LabFileRegistrationNumber registrationNumber;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "prescriber_id")
    private @Nullable String prescriberId;

    @Column(name = "preleveur_id")
    private @Nullable String preleveurId;

    @Column(nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabFileStatus status;

    @Column(name = "created_by_user_id")
    private @Nullable String createdByUserId;

    @Column(name = "insurance_id", length = 26)
    private @Nullable String insuranceId;

    @Column(name = "partner_id", length = 26)
    private @Nullable String partnerId;

    @Column(name = "home_collection_fee", nullable = false, precision = 18, scale = 2)
    private BigDecimal homeCollectionFee = BigDecimal.ZERO;

    @Column(name = "additional_fees", nullable = false, precision = 18, scale = 2)
    private BigDecimal additionalFees = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_kind", length = 10)
    private @Nullable DiscountKind discountKind;

    @Column(name = "discount_value", precision = 18, scale = 4)
    private @Nullable BigDecimal discountValue;

    @OneToMany(mappedBy = "labFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Sample> samples = new ArrayList<>();

    @OneToMany(mappedBy = "labFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AnalysisRequest> analyses = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private @Nullable Instant deletedAt;

    @Version private long version;

    /** Factory for opening a brand-new lab file in {@link LabFileStatus#OPEN}. */
    public static LabFile open(
            NewLabFile cmd,
            LabFileRegistrationNumber regNum,
            @Nullable String createdByUserId,
            Clock clock) {
        validateFees(cmd.homeCollectionFee(), cmd.additionalFees());
        validateDiscount(cmd.discountKind(), cmd.discountValue());
        var now = clock.instant();
        var f = new LabFile();
        f.id = Ulid.generate();
        f.registrationNumber = regNum;
        f.patientId = cmd.patientId();
        f.prescriberId = cmd.prescriberId();
        f.preleveurId = cmd.preleveurId();
        f.urgent = cmd.urgent();
        f.status = LabFileStatus.OPEN;
        f.createdByUserId = createdByUserId;
        f.insuranceId = cmd.insuranceId();
        f.partnerId = cmd.partnerId();
        f.homeCollectionFee = cmd.homeCollectionFee();
        f.additionalFees = cmd.additionalFees();
        f.discountKind = cmd.discountKind();
        f.discountValue = cmd.discountValue();
        f.createdAt = now;
        f.updatedAt = now;
        return f;
    }

    public LabFileId labFileId() {
        return new LabFileId(id);
    }

    public String rawId() {
        return id;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    /** Read-only view of the samples list. */
    public List<Sample> samplesView() {
        return Collections.unmodifiableList(samples);
    }

    /** Read-only view of the analysis-request list. */
    public List<AnalysisRequest> analysesView() {
        return Collections.unmodifiableList(analyses);
    }

    /**
     * Mark the file as urgent. Idempotent: returns {@code false} when already urgent (no clock
     * bump), {@code true} on first transition.
     */
    public boolean markUrgent(Clock clock) {
        if (this.urgent) {
            return false;
        }
        this.urgent = true;
        this.updatedAt = clock.instant();
        return true;
    }

    /**
     * Transitions the lab file to {@link LabFileStatus#PUBLISHED} (idempotent). Eligibility ("all
     * results biologically validated") is enforced upstream by the {@code delivery} module; the
     * aggregate owns only the lifecycle transition.
     */
    public void publish() {
        if (this.status == LabFileStatus.PUBLISHED) {
            return; // idempotent
        }
        this.status = LabFileStatus.PUBLISHED;
    }

    /**
     * Append an already-attached sample to the aggregate. Pre-condition: the caller (service) has
     * invoked {@code sample.attachToFile(this, dailyId)} BEFORE this method. The aggregate's
     * responsibility is the OPEN-status invariant + collection mutation only.
     */
    public Sample addSample(Sample sample, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        samples.add(sample);
        this.updatedAt = clock.instant();
        return sample;
    }

    /**
     * Add a new analysis request to the file. De-duplicates by {@code analysisId} — throws {@link
     * DuplicateAnalysisRequestException} when the same analysis is already on the file.
     */
    public AnalysisRequest addAnalysisRequest(NewAnalysisRequest cmd, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        boolean dup = analyses.stream().anyMatch(a -> a.getAnalysisId().equals(cmd.analysisId()));
        if (dup) {
            throw new DuplicateAnalysisRequestException(cmd.analysisCode());
        }
        var ar = AnalysisRequest.create(cmd.analysisId(), cmd.analysisCode(), clock.instant());
        ar.attachToFile(this);
        analyses.add(ar);
        this.updatedAt = clock.instant();
        return ar;
    }

    /**
     * Remove an analysis request, refusing when any sample on the file references the same
     * analysis.
     *
     * @throws AnalysisRequestNotFoundException if no matching request exists
     * @throws AnalysisHasSamplesException if a sample on the file references the same analysis
     * @throws LabFileNotEditableException if the file's status is not {@link LabFileStatus#OPEN}
     */
    public void removeAnalysisRequest(AnalysisRequestId requestId, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        var ar =
                analyses.stream()
                        .filter(a -> a.getId().equals(requestId.value()))
                        .findFirst()
                        .orElseThrow(() -> new AnalysisRequestNotFoundException(requestId));
        boolean hasSamples =
                samples.stream().anyMatch(s -> s.getAnalysisId().equals(ar.getAnalysisId()));
        if (hasSamples) {
            throw new AnalysisHasSamplesException(ar.getAnalysisCode());
        }
        analyses.remove(ar);
        this.updatedAt = clock.instant();
    }

    /**
     * Whitelist update. Each field on the command is nullable; a {@code null} sentinel means "no
     * change". Mutable: {@code urgent} / {@code prescriberId} / {@code preleveurId} + the 1.6.a
     * billing block ({@code insuranceId} / {@code partnerId} / {@code homeCollectionFee} / {@code
     * additionalFees} / {@code discountKind} / {@code discountValue}). Discount pair + non-negative
     * invariants apply on PATCH as well.
     */
    public void update(UpdateLabFile cmd, Clock clock) {
        if (status != LabFileStatus.OPEN) {
            throw new LabFileNotEditableException("file is not OPEN: " + status);
        }
        if (cmd.urgent() != null) {
            this.urgent = cmd.urgent();
        }
        if (cmd.prescriberId() != null) {
            this.prescriberId = cmd.prescriberId();
        }
        if (cmd.preleveurId() != null) {
            this.preleveurId = cmd.preleveurId();
        }
        if (cmd.insuranceId() != null) {
            this.insuranceId = cmd.insuranceId();
        }
        if (cmd.partnerId() != null) {
            this.partnerId = cmd.partnerId();
        }
        BigDecimal newHomeFee =
                cmd.homeCollectionFee() != null ? cmd.homeCollectionFee() : this.homeCollectionFee;
        BigDecimal newAdditionalFees =
                cmd.additionalFees() != null ? cmd.additionalFees() : this.additionalFees;
        validateFees(newHomeFee, newAdditionalFees);
        this.homeCollectionFee = newHomeFee;
        this.additionalFees = newAdditionalFees;
        if (cmd.discountKind() != null || cmd.discountValue() != null) {
            applyDiscount(cmd.discountKind(), cmd.discountValue());
        }
        this.updatedAt = clock.instant();
    }

    /**
     * Apply (or clear) the discount pair. Either both {@code kind} and {@code value} are {@code
     * null} (clear) or both are non-null (apply). Validates the same invariants as the DB CHECK
     * constraints: paired-or-cleared, non-negative value, {@code PERCENT} value in {@code [0, 1]}.
     */
    public void applyDiscount(@Nullable DiscountKind kind, @Nullable BigDecimal value) {
        validateDiscount(kind, value);
        this.discountKind = kind;
        this.discountValue = value;
    }

    private static void validateDiscount(@Nullable DiscountKind kind, @Nullable BigDecimal value) {
        if ((kind == null) != (value == null)) {
            throw new IllegalArgumentException(
                    "discountKind and discountValue must be both null or both set");
        }
        if (value == null) {
            return;
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("discountValue must be non-negative: " + value);
        }
        if (kind == DiscountKind.PERCENT && value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("PERCENT discountValue must be <= 1: " + value);
        }
    }

    private static void validateFees(BigDecimal homeFee, BigDecimal addFees) {
        if (homeFee.signum() < 0) {
            throw new IllegalArgumentException(
                    "homeCollectionFee must be non-negative: " + homeFee);
        }
        if (addFees.signum() < 0) {
            throw new IllegalArgumentException("additionalFees must be non-negative: " + addFees);
        }
    }
}
