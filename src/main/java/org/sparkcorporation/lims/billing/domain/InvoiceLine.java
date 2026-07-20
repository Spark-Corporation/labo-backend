package org.sparkcorporation.lims.billing.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * One row on an {@link Invoice} per priced analysis. All snapshot columns are captured at issuance
 * time from {@link org.sparkcorporation.lims.catalog.api.AnalysisSummary} and the {@link
 * org.sparkcorporation.lims.billing.domain.PricingResult.LineResult} — the line is immune to
 * subsequent catalog or pricing-config edits.
 *
 * <p>Per spec §5.1 invariant: invoices are immutable post-creation, therefore lines have NO public
 * mutators. Construction is package-private from {@link Invoice#issue}.
 */
@Entity
@Table(name = "invoice_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceLine {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "analysis_id", nullable = false)
    private String analysisId;

    @Column(name = "analysis_code_snapshot", nullable = false)
    private String analysisCodeSnapshot;

    @Column(name = "analysis_name_snapshot", nullable = false)
    private String analysisNameSnapshot;

    @Column(name = "indiciary_value_b_snapshot", nullable = false)
    private BigDecimal indiciaryValueBSnapshot;

    @Column(name = "b_rate_snapshot", nullable = false)
    private BigDecimal bRateSnapshot;

    @Column(name = "label_count_snapshot", nullable = false)
    private int labelCountSnapshot;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    static InvoiceLine create(
            Invoice invoice,
            int position,
            String analysisId,
            String analysisCodeSnapshot,
            String analysisNameSnapshot,
            BigDecimal indiciaryValueBSnapshot,
            BigDecimal bRateSnapshot,
            int labelCountSnapshot,
            BigDecimal lineTotal) {
        var l = new InvoiceLine();
        l.id = Ulid.generate();
        l.invoice = invoice;
        l.position = position;
        l.analysisId = analysisId;
        l.analysisCodeSnapshot = analysisCodeSnapshot;
        l.analysisNameSnapshot = analysisNameSnapshot;
        l.indiciaryValueBSnapshot = indiciaryValueBSnapshot;
        l.bRateSnapshot = bRateSnapshot;
        l.labelCountSnapshot = labelCountSnapshot;
        l.lineTotal = lineTotal;
        return l;
    }
}
