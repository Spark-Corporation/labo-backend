package org.sparkcorporation.lims.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceId;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.order.api.DiscountKind;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary.AnalysisRequestSummary;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Invoice aggregate root — 1:1 with {@link org.sparkcorporation.lims.order.domain.LabFile}, issued
 * once on the first {@code BiologicalValidationDone} for the file. Per spec §5.1: immutable
 * post-issuance (no {@code update*} methods, no public mutators on lines).
 *
 * <p>Construction goes through {@link #issue} only; the protected no-args constructor is reserved
 * for Hibernate.
 */
@Entity
@Table(name = "invoices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @Column(name = "number", nullable = false, unique = true, length = 20)
    private String number;

    @Column(name = "lab_file_id", nullable = false, unique = true, length = 26)
    private String labFileId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_kind", nullable = false, length = 10)
    private InvoiceCustomerKind customerKind;

    @Column(name = "customer_id", nullable = false, length = 26)
    private String customerId;

    @Column(name = "customer_name_snapshot", nullable = false, length = 255)
    private String customerNameSnapshot;

    @Column(name = "home_collection_fee_snapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal homeCollectionFeeSnapshot;

    @Column(name = "additional_fees_snapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal additionalFeesSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_kind_snapshot", length = 10)
    private @Nullable DiscountKind discountKindSnapshot;

    @Column(name = "discount_value_snapshot", precision = 18, scale = 4)
    private @Nullable BigDecimal discountValueSnapshot;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "insurance_coverage_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal insuranceCoverageAmount;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "net_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = jakarta.persistence.FetchType.LAZY)
    @OrderBy("position ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    @Version private long version;

    /**
     * Build a new {@link Invoice} aggregate. Captures snapshot columns verbatim from {@code
     * billing} + {@code pricing}; persists the {@link InvoiceLine} children in the same aggregate.
     * Per-line analysis-id / code / name snapshots are populated later via {@link
     * #populateLineSnapshots(List, Map)} once the caller has resolved the catalog rows.
     */
    public static Invoice issue(
            InvoiceNumber number,
            LabFileId labFileId,
            Customer customer,
            BillingSnapshot billing,
            PricingResult pricing,
            Instant issuedAt) {
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(labFileId, "labFileId");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(billing, "billing");
        Objects.requireNonNull(pricing, "pricing");
        Objects.requireNonNull(issuedAt, "issuedAt");

        if (pricing.netAmount().signum() < 0) {
            throw new InvalidPricingException("net amount must be ≥ 0; got " + pricing.netAmount());
        }

        var i = new Invoice();
        i.id = Ulid.generate();
        i.number = number.value();
        i.labFileId = labFileId.value();
        i.issuedAt = issuedAt;
        i.customerKind = customer.kind();
        i.customerId = customer.id();
        i.customerNameSnapshot = customer.nameSnapshot();
        i.homeCollectionFeeSnapshot = billing.homeCollectionFee();
        i.additionalFeesSnapshot = billing.additionalFees();
        i.discountKindSnapshot = billing.discountKind();
        i.discountValueSnapshot = billing.discountValue();
        i.totalAmount = pricing.totalAmount();
        i.insuranceCoverageAmount = pricing.insuranceCoverageAmount();
        i.discountAmount = pricing.discountAmount();
        i.netAmount = pricing.netAmount();
        i.createdAt = issuedAt;
        return i;
    }

    /**
     * Populate per-line snapshot rows from the parallel {@code requests} (positional) + {@code
     * analyses} (by id) inputs and the previously-set {@code pricing} totals. This is split out of
     * {@link #issue} because pricing is a pure domain computation (no AnalysisSummary), but each
     * line needs the catalog snapshot (code, name).
     *
     * <p>Positional alignment: {@code requests[i]} pairs with {@code pricing.lines()[i]}. The
     * caller (typically {@code BillingFinalizationService}) is responsible for that ordering.
     */
    public void populateLineSnapshots(
            List<AnalysisRequestSummary> requests,
            Map<AnalysisId, AnalysisSummary> analyses,
            PricingResult pricing) {
        if (!this.lines.isEmpty()) {
            throw new IllegalStateException("invoice " + id + " already has lines");
        }
        if (requests.size() != pricing.lines().size()) {
            throw new IllegalArgumentException(
                    "requests size ("
                            + requests.size()
                            + ") must equal pricing.lines size ("
                            + pricing.lines().size()
                            + ")");
        }
        for (int i = 0; i < requests.size(); i++) {
            var req = requests.get(i);
            var lineResult = pricing.lines().get(i);
            var analysisId = AnalysisId.of(req.analysisId());
            var analysis = analyses.get(analysisId);
            if (analysis == null) {
                throw new IllegalArgumentException(
                        "analysis " + req.analysisId() + " not in resolved analyses map");
            }
            var line =
                    InvoiceLine.create(
                            this,
                            lineResult.position(),
                            req.analysisId(),
                            analysis.code(),
                            analysis.name(),
                            analysis.indiciaryValueB(),
                            lineResult.bRate(),
                            analysis.labelCount(),
                            lineResult.lineTotal());
            this.lines.add(line);
        }
    }

    public InvoiceId invoiceId() {
        return InvoiceId.of(id);
    }

    public InvoiceNumber invoiceNumber() {
        return new InvoiceNumber(number);
    }

    public LabFileId labFile() {
        return new LabFileId(labFileId);
    }

    public List<InvoiceLine> linesView() {
        return Collections.unmodifiableList(lines);
    }
}
