package org.sparkcorporation.lims.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.QuoteId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Quote (devis) aggregate root — soft-deleted via Hibernate {@link SQLDelete}, with explicit
 * version-aware UPDATE to preserve optimistic-lock semantics on concurrent deletes.
 *
 * <p>Per spec §4.4: deliberately NO {@code @SQLRestriction} so historical FK integrity from
 * converted lab files is preserved; repositories explicitly filter {@code deleted_at IS NULL}.
 *
 * <p>Lifecycle: {@code DRAFT → LOCKED → CONVERTED} (spec §5.1). DRAFT-only mutators raise {@link
 * QuoteNotEditableException} when invoked on a non-DRAFT quote; {@code :convert} raises {@link
 * QuoteAlreadyConvertedException} on a CONVERTED quote (precedence over the generic not-editable).
 * Pricing snapshots are written exactly once at {@code :lock} via the package-private {@link
 * QuoteLine#snapshotPricing(BigDecimal, BigDecimal)} mutator — kept off the public surface so a
 * second {@code :lock} cannot bypass the status guard.
 */
@Entity
@Table(name = "quotes")
@SQLDelete(
        sql =
                "UPDATE quotes SET deleted_at = now(), version = version + 1 WHERE id = ? AND"
                        + " version = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quote {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "quote_number"))
    private QuoteNumber quoteNumber;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "prescriber_id")
    private @Nullable String prescriberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status;

    @Column(name = "locked_at")
    private @Nullable Instant lockedAt;

    @Column(name = "lab_file_id")
    private @Nullable String labFileId;

    @Column(name = "converted_at")
    private @Nullable Instant convertedAt;

    @Column(name = "expires_at")
    private @Nullable Instant expiresAt;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "created_by_user_id")
    private @Nullable String createdByUserId;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<QuoteLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private @Nullable Instant deletedAt;

    @Version private long version;

    /** Factory for opening a brand-new quote at status {@code DRAFT} with zero total. */
    public static Quote draft(
            NewQuote cmd, QuoteNumber quoteNumber, @Nullable String createdByUserId, Clock clock) {
        var now = clock.instant();
        var q = new Quote();
        q.id = Ulid.generate();
        q.quoteNumber = quoteNumber;
        q.patientId = cmd.patientId();
        q.prescriberId = cmd.prescriberId();
        q.expiresAt = cmd.expiresAt();
        q.status = QuoteStatus.DRAFT;
        q.totalAmount = BigDecimal.ZERO;
        q.createdByUserId = createdByUserId;
        q.createdAt = now;
        q.updatedAt = now;
        return q;
    }

    public QuoteId quoteId() {
        return new QuoteId(id);
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    /** Read-only view of the lines list. */
    public List<QuoteLine> linesView() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Add an analysis line to a DRAFT quote. Refuses duplicate {@code analysisId} on the same quote
     * (mirrors {@link LabFile#addAnalysisRequest(NewAnalysisRequest, java.time.Clock)}). The {@link
     * QuoteLine#bRateSnapshot} / {@link QuoteLine#lineTotal} fields stay at zero until {@code
     * :lock}.
     */
    public QuoteLine addLine(NewQuoteLine cmd, Clock clock) {
        if (status != QuoteStatus.DRAFT) {
            throw new QuoteNotEditableException("quote must be DRAFT to add lines; was " + status);
        }
        boolean dup = lines.stream().anyMatch(l -> l.getAnalysisId().equals(cmd.analysisId()));
        if (dup) {
            throw new DuplicateAnalysisRequestException(cmd.analysisCode());
        }
        var line =
                QuoteLine.create(
                        this,
                        cmd.analysisId(),
                        cmd.analysisCode(),
                        cmd.indiciaryValueBSnapshot(),
                        cmd.labelCountSnapshot());
        lines.add(line);
        this.updatedAt = clock.instant();
        return line;
    }

    /**
     * Remove a line from a DRAFT quote.
     *
     * @throws QuoteLineNotFoundException if no matching line exists on this quote
     * @throws QuoteNotEditableException if the quote's status is not DRAFT
     */
    public void removeLine(QuoteLineId lineId, Clock clock) {
        if (status != QuoteStatus.DRAFT) {
            throw new QuoteNotEditableException(
                    "quote must be DRAFT to remove lines; was " + status);
        }
        var line =
                lines.stream()
                        .filter(l -> l.getId().equals(lineId.value()))
                        .findFirst()
                        .orElseThrow(() -> new QuoteLineNotFoundException(lineId));
        lines.remove(line);
        this.updatedAt = clock.instant();
    }

    /**
     * Whitelist update — each field on the command is nullable; a {@code null} sentinel means "no
     * change". Only {@code prescriberId} / {@code expiresAt} are touched through this path
     * (whitelist enforced at the controller layer per spec §5.4).
     */
    public void update(UpdateQuote cmd, Clock clock) {
        if (status != QuoteStatus.DRAFT) {
            throw new QuoteNotEditableException("quote must be DRAFT to update; was " + status);
        }
        if (cmd.prescriberId() != null) {
            this.prescriberId = cmd.prescriberId();
        }
        if (cmd.expiresAt() != null) {
            this.expiresAt = cmd.expiresAt();
        }
        this.updatedAt = clock.instant();
    }

    /**
     * Lock the quote: snapshot {@code bRate} on every line, compute each {@code lineTotal} via
     * {@code indiciaryValueB × bRate × labelCount} (HALF_UP to 2 dp), set the aggregate's {@code
     * totalAmount} to the exact arithmetic sum of the rounded line-totals.
     *
     * @throws QuoteNotEditableException if the quote's status is not DRAFT (a second {@code :lock}
     *     attempt is rejected here, before any line mutator runs)
     * @throws EmptyQuoteException if the quote has zero lines
     */
    public void lock(BigDecimal currentBRate, Clock clock) {
        if (status != QuoteStatus.DRAFT) {
            throw new QuoteNotEditableException("quote must be DRAFT to lock; was " + status);
        }
        if (lines.isEmpty()) {
            throw new EmptyQuoteException("cannot lock an empty quote");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var line : lines) {
            var lineTotal =
                    line.getIndiciaryValueBSnapshot()
                            .multiply(currentBRate)
                            .multiply(BigDecimal.valueOf(line.getLabelCountSnapshot()))
                            .setScale(2, RoundingMode.HALF_UP);
            line.snapshotPricing(currentBRate, lineTotal);
            total = total.add(lineTotal);
        }
        this.totalAmount = total;
        this.status = QuoteStatus.LOCKED;
        this.lockedAt = clock.instant();
        this.updatedAt = clock.instant();
    }

    /**
     * Move a LOCKED quote to CONVERTED, recording the new lab-file id. Per spec §5.5 precedence:
     * the {@code CONVERTED} branch is checked BEFORE the generic {@code != LOCKED} branch so
     * already-converted quotes raise the distinct {@link QuoteAlreadyConvertedException} (with the
     * surviving {@code labFileId} accessor) for clearer client error UX.
     */
    public void convert(LabFileId labFileId, Clock clock) {
        if (status == QuoteStatus.CONVERTED) {
            throw new QuoteAlreadyConvertedException(quoteId(), this.labFileId);
        }
        if (status != QuoteStatus.LOCKED) {
            throw new QuoteNotEditableException("quote must be LOCKED to convert; was " + status);
        }
        this.labFileId = labFileId.value();
        this.convertedAt = clock.instant();
        this.status = QuoteStatus.CONVERTED;
        this.updatedAt = clock.instant();
    }
}
