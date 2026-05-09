package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.order.api.QuoteLineId;
import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.api.QuoteStatus;
import org.sparkcorporation.lims.shared.id.Ulid;

class QuoteTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-05-08T10:00:00Z"), ZoneOffset.UTC);
    private static final Clock LATER_CLOCK =
            Clock.fixed(Instant.parse("2026-05-08T11:00:00Z"), ZoneOffset.UTC);
    private static final BigDecimal B_RATE = new BigDecimal("400");
    private static final QuoteNumber QN = new QuoteNumber("DEV-260508-0001");

    @Test
    void lock_rejects_when_not_DRAFT() throws Exception {
        var q = draft();
        addOneLine(q);
        forceStatus(q, QuoteStatus.LOCKED);

        assertThatThrownBy(() -> q.lock(B_RATE, CLOCK))
                .isInstanceOf(QuoteNotEditableException.class)
                .hasMessageContaining("must be DRAFT");
    }

    @Test
    void factory_starts_in_DRAFT_with_zero_total() {
        var q = draft();
        assertThat(q.getStatus()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(q.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(q.getQuoteNumber()).isEqualTo(QN);
        assertThat(q.isActive()).isTrue();
        assertThat(q.getLockedAt()).isNull();
        assertThat(q.getLabFileId()).isNull();
        assertThat(q.getConvertedAt()).isNull();
        assertThat(q.getCreatedAt()).isEqualTo(CLOCK.instant());
        assertThat(q.getUpdatedAt()).isEqualTo(CLOCK.instant());
        assertThat(q.getCreatedByUserId()).isEqualTo(validUlid("CREATOR"));
        assertThat(q.getId()).hasSize(26);
        assertThat(q.getLines()).isEmpty();
    }

    @Test
    void addLine_rejects_when_not_DRAFT() throws Exception {
        var q = draft();
        forceStatus(q, QuoteStatus.LOCKED);
        var cmd = sampleLineCmd("01ANALYSIS");

        assertThatThrownBy(() -> q.addLine(cmd, CLOCK))
                .isInstanceOf(QuoteNotEditableException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void addLine_rejects_duplicate_analysisId() {
        var q = draft();
        var cmd = sampleLineCmd("01ANALYSIS");
        q.addLine(cmd, CLOCK);

        assertThatThrownBy(() -> q.addLine(sampleLineCmd("01ANALYSIS"), CLOCK))
                .isInstanceOf(DuplicateAnalysisRequestException.class);
    }

    @Test
    void removeLine_rejects_when_not_DRAFT() throws Exception {
        var q = draft();
        var line = q.addLine(sampleLineCmd("01ANALYSIS"), CLOCK);
        forceStatus(q, QuoteStatus.LOCKED);

        var lineId = new QuoteLineId(line.getId());
        assertThatThrownBy(() -> q.removeLine(lineId, CLOCK))
                .isInstanceOf(QuoteNotEditableException.class);
    }

    @Test
    void removeLine_throws_when_not_found() {
        var q = draft();
        var unknown = QuoteLineId.next();
        assertThatThrownBy(() -> q.removeLine(unknown, CLOCK))
                .isInstanceOf(QuoteLineNotFoundException.class);
    }

    @Test
    void update_rejects_when_not_DRAFT() throws Exception {
        var q = draft();
        forceStatus(q, QuoteStatus.LOCKED);

        var cmd = new UpdateQuote(validUlid("PRESCRIBER"), null);
        assertThatThrownBy(() -> q.update(cmd, CLOCK))
                .isInstanceOf(QuoteNotEditableException.class);
    }

    @Test
    void update_with_null_fields_is_no_change() {
        var q = draft();
        var originalPrescriber = q.getPrescriberId();
        var originalExpires = q.getExpiresAt();

        q.update(new UpdateQuote(null, null), LATER_CLOCK);

        assertThat(q.getPrescriberId()).isEqualTo(originalPrescriber);
        assertThat(q.getExpiresAt()).isEqualTo(originalExpires);
        // updatedAt always bumps; null-fields means "no field change", not "no clock bump"
        assertThat(q.getUpdatedAt()).isEqualTo(LATER_CLOCK.instant());
    }

    @Test
    void lock_rejects_empty_quote_with_EmptyQuoteException() {
        var q = draft();
        assertThatThrownBy(() -> q.lock(B_RATE, CLOCK))
                .isInstanceOf(EmptyQuoteException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void lock_computes_lineTotal_indiciaryValueB_times_bRate_times_labelCount_HALF_UP() {
        var q = draft();
        // 12.345 × 400 × 1 = 4938.000 → HALF_UP to 2 dp = 4938.00
        q.addLine(
                new NewQuoteLine(validUlid("ANALYSIS1"), "GLUC", new BigDecimal("12.345"), 1),
                CLOCK);
        q.lock(B_RATE, LATER_CLOCK);

        assertThat(q.getLines()).hasSize(1);
        assertThat(q.getLines().get(0).getLineTotal())
                .isEqualByComparingTo(new BigDecimal("4938.00"));
        assertThat(q.getLines().get(0).getBRateSnapshot()).isEqualByComparingTo(B_RATE);
        assertThat(q.getStatus()).isEqualTo(QuoteStatus.LOCKED);
        assertThat(q.getLockedAt()).isEqualTo(LATER_CLOCK.instant());
    }

    @Test
    void lock_total_is_exact_sum_of_rounded_lineTotals() {
        var q = draft();
        // Two lines with values that don't round trivially.
        q.addLine(
                new NewQuoteLine(validUlid("ANALYSIS1"), "AAA", new BigDecimal("0.125"), 1),
                CLOCK); // 0.125 × 400 × 1 = 50.000 → 50.00
        q.addLine(
                new NewQuoteLine(validUlid("ANALYSIS2"), "BBB", new BigDecimal("0.005"), 3),
                CLOCK); // 0.005 × 400 × 3 = 6.000 → 6.00
        q.lock(B_RATE, LATER_CLOCK);

        assertThat(q.getLines().get(0).getLineTotal())
                .isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(q.getLines().get(1).getLineTotal()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(q.getTotalAmount()).isEqualByComparingTo(new BigDecimal("56.00"));
    }

    @Test
    void lock_already_LOCKED_throws_QuoteNotEditable() {
        var q = draft();
        addOneLine(q);
        q.lock(B_RATE, LATER_CLOCK);

        assertThatThrownBy(() -> q.lock(B_RATE, LATER_CLOCK))
                .isInstanceOf(QuoteNotEditableException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void convert_rejects_when_not_LOCKED() {
        var q = draft();
        addOneLine(q);
        // status still DRAFT
        var labFileId = LabFileId.next();
        assertThatThrownBy(() -> q.convert(labFileId, CLOCK))
                .isInstanceOf(QuoteNotEditableException.class)
                .hasMessageContaining("LOCKED");
    }

    @Test
    void convert_already_CONVERTED_throws_QuoteAlreadyConverted_with_labFileId_accessor() {
        var q = draft();
        addOneLine(q);
        q.lock(B_RATE, LATER_CLOCK);
        var firstLabFile = LabFileId.next();
        q.convert(firstLabFile, LATER_CLOCK);

        var secondLabFile = LabFileId.next();
        assertThatThrownBy(() -> q.convert(secondLabFile, LATER_CLOCK))
                .isInstanceOf(QuoteAlreadyConvertedException.class)
                .satisfies(
                        ex ->
                                assertThat(((QuoteAlreadyConvertedException) ex).labFileId())
                                        .isEqualTo(firstLabFile.value()));
    }

    @Test
    void convert_LOCKED_to_CONVERTED_sets_labFileId_and_convertedAt() {
        var q = draft();
        addOneLine(q);
        q.lock(B_RATE, LATER_CLOCK);

        var labFileId = LabFileId.next();
        q.convert(labFileId, LATER_CLOCK);

        assertThat(q.getStatus()).isEqualTo(QuoteStatus.CONVERTED);
        assertThat(q.getLabFileId()).isEqualTo(labFileId.value());
        assertThat(q.getConvertedAt()).isEqualTo(LATER_CLOCK.instant());
    }

    private Quote draft() {
        return Quote.draft(
                new NewQuote(validUlid("PATIENT"), null, null, List.of()),
                QN,
                validUlid("CREATOR"),
                CLOCK);
    }

    private NewQuoteLine sampleLineCmd(String idPrefix) {
        return new NewQuoteLine(validUlid(idPrefix), "GLUC", new BigDecimal("10.00"), 1);
    }

    private void addOneLine(Quote q) {
        q.addLine(sampleLineCmd("01ANALYSIS"), CLOCK);
    }

    /**
     * Memoizes a stable ULID per label so calls with the same label inside a single test resolve to
     * the same value (required for dedup-coverage cases where two {@code addLine} commands must
     * share an {@code analysisId}). The memo is per-test-method via a fresh map; the underlying
     * ULIDs are still validly-formatted.
     */
    private final Map<String, String> ulidCache = new HashMap<>();

    private String validUlid(String label) {
        return ulidCache.computeIfAbsent(label, k -> Ulid.generate());
    }

    private static void forceStatus(Quote q, QuoteStatus s) throws Exception {
        Field f = Quote.class.getDeclaredField("status");
        f.setAccessible(true);
        f.set(q, s);
    }
}
