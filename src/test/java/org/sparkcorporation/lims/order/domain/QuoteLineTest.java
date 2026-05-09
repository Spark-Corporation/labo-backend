package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Verifies {@link QuoteLine} snapshot semantics: pricing fields are zero on creation and only
 * populated post-{@code Quote.lock(...)}. The mutator that flips them is package-private — there is
 * no public setter to assert "doesn't exist", so the absence is enforced by visibility at compile
 * time.
 */
class QuoteLineTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-08T10:00:00Z"), ZoneOffset.UTC);
    private static final QuoteNumber QN = new QuoteNumber("DEV-260508-0001");
    private static final BigDecimal B_RATE = new BigDecimal("400");

    @Test
    void create_starts_with_zero_bRate_and_zero_lineTotal() {
        var quote = freshQuote();
        var line =
                quote.addLine(
                        new NewQuoteLine(Ulid.generate(), "GLUC", new BigDecimal("10.00"), 1),
                        CLOCK);

        assertThat(line.getBRateSnapshot()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line.getLineTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(line.getIndiciaryValueBSnapshot()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(line.getLabelCountSnapshot()).isEqualTo(1);
        assertThat(line.getAnalysisCode()).isEqualTo("GLUC");
        assertThat(line.getId()).hasSize(26);
        assertThat(line.getQuote()).isSameAs(quote);
    }

    @Test
    void accessors_expose_immutable_snapshots_after_lock() {
        var quote = freshQuote();
        var line =
                quote.addLine(
                        new NewQuoteLine(Ulid.generate(), "GLUC", new BigDecimal("10.00"), 1),
                        CLOCK);

        quote.lock(B_RATE, CLOCK);

        // post-lock the line's pricing fields are populated
        assertThat(line.getBRateSnapshot()).isEqualByComparingTo(B_RATE);
        // 10.00 × 400 × 1 = 4000.00
        assertThat(line.getLineTotal()).isEqualByComparingTo(new BigDecimal("4000.00"));
        // and the source (indiciaryValueB / labelCount) snapshots are unchanged
        assertThat(line.getIndiciaryValueBSnapshot()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(line.getLabelCountSnapshot()).isEqualTo(1);
    }

    private static Quote freshQuote() {
        return Quote.draft(
                new NewQuote(Ulid.generate(), null, null, List.of()),
                QN,
                /* createdByUserId */ null,
                CLOCK);
    }
}
