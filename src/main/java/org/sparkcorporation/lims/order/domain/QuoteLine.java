package org.sparkcorporation.lims.order.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * One analysis on a {@link Quote}. {@code analysisCode} / {@code indiciaryValueBSnapshot} / {@code
 * labelCountSnapshot} are captured at line creation (post-{@code AnalysisApi.findByCode}
 * resolution) — the line is immune to dictionary edits after that.
 *
 * <p>{@code bRateSnapshot} / {@code lineTotal} are written exactly once, at {@code Quote.lock()}
 * time, via the package-private {@link #snapshotPricing(BigDecimal, BigDecimal)} mutator. Per spec
 * §5.2: visibility ({@code package-private}) + the aggregate's status guard together kill the
 * double-lock class of bugs — there is no public mutator for these fields, and any second {@code
 * Quote.lock} call is rejected by the {@code status != DRAFT} branch before it reaches the line.
 */
@Entity
@Table(name = "quote_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuoteLine {

    @Id
    @Column(columnDefinition = "varchar(26)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "analysis_id", nullable = false)
    private String analysisId;

    @Column(name = "analysis_code", nullable = false)
    private String analysisCode;

    @Column(name = "indiciary_value_b_snapshot", nullable = false)
    private BigDecimal indiciaryValueBSnapshot;

    @Column(name = "b_rate_snapshot", nullable = false)
    private BigDecimal bRateSnapshot;

    @Column(name = "label_count_snapshot", nullable = false)
    private int labelCountSnapshot;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Version private long version;

    static QuoteLine create(
            Quote quote,
            String analysisId,
            String analysisCode,
            BigDecimal indiciaryValueBSnapshot,
            int labelCountSnapshot) {
        var l = new QuoteLine();
        l.id = Ulid.generate();
        l.quote = quote;
        l.analysisId = analysisId;
        l.analysisCode = analysisCode;
        l.indiciaryValueBSnapshot = indiciaryValueBSnapshot;
        l.bRateSnapshot = BigDecimal.ZERO;
        l.labelCountSnapshot = labelCountSnapshot;
        l.lineTotal = BigDecimal.ZERO;
        return l;
    }

    /**
     * Package-private mutator called exclusively from {@link Quote#lock(BigDecimal,
     * java.time.Clock)}. Per spec §5.2 invariant: written exactly once. Visibility prevents bypass;
     * the entity exposes no public mutator for these fields.
     */
    void snapshotPricing(BigDecimal bRate, BigDecimal lineTotal) {
        this.bRateSnapshot = bRate;
        this.lineTotal = lineTotal;
    }
}
