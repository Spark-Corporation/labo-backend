package org.sparkcorporation.lims.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.billing.api.InvoiceCustomerKind;
import org.sparkcorporation.lims.billing.api.InvoiceNumber;
import org.sparkcorporation.lims.billing.domain.Customer.PatientCustomer;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.AnalyticalDelaySummary;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.DisciplineSnapshot;
import org.sparkcorporation.lims.catalog.api.AnalysisSummary.UnitsSummary;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.LabFileSummary.AnalysisRequestSummary;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Unit tests for {@link Invoice} invariants per spec §5.1. Covers the static factory + line
 * population path; the {@link InvoiceNumberGenerator}-driven number generation lives in its own
 * test class. Negative tests assert the immutability + line-population contract.
 */
class InvoiceTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-05-13T10:00:00Z");
    private static final LabFileId LAB_FILE = new LabFileId(Ulid.generate());

    @Test
    void issue_captures_snapshot_columns_verbatim() {
        var pricing =
                new PricingResult(
                        new BigDecimal("120000"),
                        new BigDecimal("96000"),
                        new BigDecimal("5000"),
                        new BigDecimal("19000"),
                        List.of(
                                new PricingResult.LineResult(
                                        1, new BigDecimal("1500"), new BigDecimal("75000")),
                                new PricingResult.LineResult(
                                        2, new BigDecimal("1500"), new BigDecimal("45000"))));
        var billing = new BillingSnapshot(new BigDecimal("0"), new BigDecimal("0"), null, null);
        var customer = new PatientCustomer(Ulid.generate(), "LAB-A DOE");

        var invoice =
                Invoice.issue(
                        new InvoiceNumber("LAB26-1305-001"),
                        LAB_FILE,
                        customer,
                        billing,
                        pricing,
                        ISSUED_AT);

        assertThat(invoice.getNumber()).isEqualTo("LAB26-1305-001");
        assertThat(invoice.getLabFileId()).isEqualTo(LAB_FILE.value());
        assertThat(invoice.getIssuedAt()).isEqualTo(ISSUED_AT);
        assertThat(invoice.getCustomerKind()).isEqualTo(InvoiceCustomerKind.PATIENT);
        assertThat(invoice.getCustomerNameSnapshot()).isEqualTo("LAB-A DOE");
        assertThat(invoice.getHomeCollectionFeeSnapshot()).isEqualByComparingTo("0");
        assertThat(invoice.getAdditionalFeesSnapshot()).isEqualByComparingTo("0");
        assertThat(invoice.getDiscountKindSnapshot()).isNull();
        assertThat(invoice.getDiscountValueSnapshot()).isNull();
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("120000");
        assertThat(invoice.getInsuranceCoverageAmount()).isEqualByComparingTo("96000");
        assertThat(invoice.getDiscountAmount()).isEqualByComparingTo("5000");
        assertThat(invoice.getNetAmount()).isEqualByComparingTo("19000");
        assertThat(invoice.getCreatedAt()).isEqualTo(ISSUED_AT);
    }

    @Test
    void issue_rejects_negative_net_amount() {
        var pricing =
                new PricingResult(
                        new BigDecimal("1000"),
                        new BigDecimal("500"),
                        new BigDecimal("600"),
                        new BigDecimal("-100"),
                        List.of());
        assertThatThrownBy(
                        () ->
                                Invoice.issue(
                                        new InvoiceNumber("LAB26-1305-001"),
                                        LAB_FILE,
                                        new PatientCustomer(Ulid.generate(), "X Y"),
                                        new BillingSnapshot(
                                                BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                                        pricing,
                                        ISSUED_AT))
                .isInstanceOf(InvalidPricingException.class);
    }

    @Test
    void populate_line_snapshots_aligns_requests_with_pricing_lines() {
        var aid1 = AnalysisId.generate();
        var aid2 = AnalysisId.generate();
        var pricing =
                new PricingResult(
                        new BigDecimal("120000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("120000"),
                        List.of(
                                new PricingResult.LineResult(
                                        1, new BigDecimal("1500"), new BigDecimal("75000")),
                                new PricingResult.LineResult(
                                        2, new BigDecimal("1500"), new BigDecimal("45000"))));
        var invoice =
                Invoice.issue(
                        new InvoiceNumber("LAB26-1305-001"),
                        LAB_FILE,
                        new PatientCustomer(Ulid.generate(), "LAB-A DOE"),
                        new BillingSnapshot(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                        pricing,
                        ISSUED_AT);

        var req1 =
                new AnalysisRequestSummary(
                        new AnalysisRequestId(Ulid.generate()), aid1.value(), "GLUC", ISSUED_AT);
        var req2 =
                new AnalysisRequestSummary(
                        new AnalysisRequestId(Ulid.generate()), aid2.value(), "CREA", ISSUED_AT);

        var analyses =
                Map.of(
                        aid1, summary(aid1, "GLUC", "Glucose", new BigDecimal("50"), 1),
                        aid2, summary(aid2, "CREA", "Creatinine", new BigDecimal("30"), 1));

        invoice.populateLineSnapshots(List.of(req1, req2), analyses, pricing);

        var lines = invoice.linesView();
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getPosition()).isEqualTo(1);
        assertThat(lines.get(0).getAnalysisCodeSnapshot()).isEqualTo("GLUC");
        assertThat(lines.get(0).getAnalysisNameSnapshot()).isEqualTo("Glucose");
        assertThat(lines.get(0).getIndiciaryValueBSnapshot()).isEqualByComparingTo("50");
        assertThat(lines.get(0).getBRateSnapshot()).isEqualByComparingTo("1500");
        assertThat(lines.get(0).getLabelCountSnapshot()).isEqualTo(1);
        assertThat(lines.get(0).getLineTotal()).isEqualByComparingTo("75000");
        assertThat(lines.get(1).getPosition()).isEqualTo(2);
        assertThat(lines.get(1).getAnalysisCodeSnapshot()).isEqualTo("CREA");
    }

    @Test
    void populate_line_snapshots_is_one_shot() {
        var aid = AnalysisId.generate();
        var pricing =
                new PricingResult(
                        new BigDecimal("75000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("75000"),
                        List.of(
                                new PricingResult.LineResult(
                                        1, new BigDecimal("1500"), new BigDecimal("75000"))));
        var invoice =
                Invoice.issue(
                        new InvoiceNumber("LAB26-0513-002"),
                        LAB_FILE,
                        new PatientCustomer(Ulid.generate(), "LAB-A DOE"),
                        new BillingSnapshot(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                        pricing,
                        ISSUED_AT);
        var req =
                new AnalysisRequestSummary(
                        new AnalysisRequestId(Ulid.generate()), aid.value(), "GLUC", ISSUED_AT);
        var analyses = Map.of(aid, summary(aid, "GLUC", "Glucose", new BigDecimal("50"), 1));
        invoice.populateLineSnapshots(List.of(req), analyses, pricing);

        assertThatThrownBy(() -> invoice.populateLineSnapshots(List.of(req), analyses, pricing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has lines");
    }

    @Test
    void populate_line_snapshots_rejects_size_mismatch() {
        var aid = AnalysisId.generate();
        var pricing =
                new PricingResult(
                        new BigDecimal("75000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("75000"),
                        List.of(
                                new PricingResult.LineResult(
                                        1, new BigDecimal("1500"), new BigDecimal("75000")),
                                new PricingResult.LineResult(
                                        2, new BigDecimal("1500"), new BigDecimal("45000"))));
        var invoice =
                Invoice.issue(
                        new InvoiceNumber("LAB26-0513-003"),
                        LAB_FILE,
                        new PatientCustomer(Ulid.generate(), "LAB-A DOE"),
                        new BillingSnapshot(BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                        pricing,
                        ISSUED_AT);
        var req =
                new AnalysisRequestSummary(
                        new AnalysisRequestId(Ulid.generate()), aid.value(), "GLUC", ISSUED_AT);
        assertThatThrownBy(
                        () ->
                                invoice.populateLineSnapshots(
                                        List.of(req),
                                        Map.of(
                                                aid,
                                                summary(
                                                        aid,
                                                        "GLUC",
                                                        "Glucose",
                                                        new BigDecimal("50"),
                                                        1)),
                                        pricing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must equal");
    }

    @Test
    void factory_rejects_null_inputs() {
        var pricing =
                new PricingResult(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of());
        assertThatThrownBy(
                        () ->
                                Invoice.issue(
                                        null,
                                        LAB_FILE,
                                        new PatientCustomer(Ulid.generate(), "X"),
                                        new BillingSnapshot(
                                                BigDecimal.ZERO, BigDecimal.ZERO, null, null),
                                        pricing,
                                        ISSUED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("number");
    }

    private static AnalysisSummary summary(
            AnalysisId id, String code, String name, BigDecimal indiciaryValueB, int labelCount) {
        return new AnalysisSummary(
                id,
                code,
                name,
                AnalysisKind.SIMPLE,
                indiciaryValueB,
                labelCount,
                new DisciplineSnapshot(
                        new DisciplineId(Ulid.generate()),
                        "BIO",
                        "Biochimie",
                        DisciplinePrefix.BIOCHIMIE),
                null,
                null,
                ResultType.REAL,
                null,
                null,
                null,
                null,
                new UnitsSummary(null, null, null, null, null),
                new AnalyticalDelaySummary(null, null, null),
                List.of(),
                List.of());
    }
}
