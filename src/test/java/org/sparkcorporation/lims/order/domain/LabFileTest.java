package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.DiscountKind;
import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;
import org.sparkcorporation.lims.order.api.SampleDailyId;

class LabFileTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-07T08:00:00Z"), ZoneOffset.UTC);
    private static final LabFileRegistrationNumber REG =
            new LabFileRegistrationNumber("LF-260507-0001");
    private static final SampleDailyId DAILY = new SampleDailyId("1070526-001");

    private static NewLabFile newCmd() {
        return new NewLabFile(
                "01PATIENT",
                "01PRESCRIBER",
                "01PRELEVEUR",
                false,
                List.of("GLUC"),
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null);
    }

    @Test
    void open_initializes_status_to_OPEN_and_audit_columns() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);

        assertThat(file.getStatus()).isEqualTo(LabFileStatus.OPEN);
        assertThat(file.getRegistrationNumber()).isEqualTo(REG);
        assertThat(file.getPatientId()).isEqualTo("01PATIENT");
        assertThat(file.isUrgent()).isFalse();
        assertThat(file.isActive()).isTrue();
        assertThat(file.getCreatedAt()).isEqualTo(FIXED.instant());
        assertThat(file.getUpdatedAt()).isEqualTo(FIXED.instant());
        assertThat(file.getCreatedByUserId()).isEqualTo("01CREATOR");
        assertThat(file.rawId()).isNotNull().hasSize(26);
    }

    @Test
    void open_allows_null_creator() {
        var file = LabFile.open(newCmd(), REG, /* createdByUserId */ null, FIXED);
        assertThat(file.getCreatedByUserId()).isNull();
    }

    @Test
    void newLabFile_command_copies_analysis_codes_defensively() {
        var src = new java.util.ArrayList<>(List.of("GLUC", "CHOL"));
        var cmd =
                new NewLabFile(
                        "p",
                        null,
                        null,
                        false,
                        src,
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        src.clear();
        assertThat(cmd.analysisCodes()).containsExactly("GLUC", "CHOL");
    }

    @Test
    void markUrgent_first_call_returns_true_and_bumps_clock() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        var laterClock = Clock.fixed(Instant.parse("2026-05-07T09:00:00Z"), ZoneOffset.UTC);

        boolean changed = file.markUrgent(laterClock);

        assertThat(changed).isTrue();
        assertThat(file.isUrgent()).isTrue();
        assertThat(file.getUpdatedAt()).isEqualTo(laterClock.instant());
    }

    @Test
    void markUrgent_idempotent_when_already_urgent_does_not_bump_clock() {
        var openCmd =
                new NewLabFile(
                        "01PATIENT",
                        null,
                        null,
                        true,
                        List.of("GLUC"),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        var file = LabFile.open(openCmd, REG, "01CREATOR", FIXED);
        var laterClock = Clock.fixed(Instant.parse("2026-05-07T09:00:00Z"), ZoneOffset.UTC);

        boolean changed = file.markUrgent(laterClock);

        assertThat(changed).isFalse();
        assertThat(file.getUpdatedAt()).isEqualTo(FIXED.instant()); // unchanged
    }

    @Test
    void addAnalysisRequest_appends_when_not_duplicate() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);

        var ar = file.addAnalysisRequest(new NewAnalysisRequest("01ANALYSIS", "GLUC"), FIXED);

        assertThat(file.analysesView()).hasSize(1);
        assertThat(ar.getAnalysisCode()).isEqualTo("GLUC");
        assertThat(ar.getLabFile()).isSameAs(file);
    }

    @Test
    void addAnalysisRequest_throws_DuplicateAnalysisRequestException_on_same_analysisId() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        file.addAnalysisRequest(new NewAnalysisRequest("01ANALYSIS", "GLUC"), FIXED);

        assertThatThrownBy(
                        () ->
                                file.addAnalysisRequest(
                                        new NewAnalysisRequest("01ANALYSIS", "GLUC"), FIXED))
                .isInstanceOf(DuplicateAnalysisRequestException.class);
    }

    @Test
    void addSample_rejects_when_status_not_OPEN() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        forceStatus(file, LabFileStatus.AWAITING_RESULTS);

        var sample = InLabSample.create("01ANALYSIS", "01RECIPIENT", FIXED.instant(), "01USER");

        assertThatThrownBy(() -> file.addSample(sample, FIXED))
                .isInstanceOf(LabFileNotEditableException.class)
                .hasMessageContaining("AWAITING_RESULTS");
    }

    @Test
    void addSample_appends_when_OPEN() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        var sample = InLabSample.create("01ANALYSIS", "01RECIPIENT", FIXED.instant(), "01USER");
        // Service-side wiring (per chunk-1 code-review fix): caller attaches BEFORE addSample.
        sample.attachToFile(file, DAILY);

        file.addSample(sample, FIXED);

        assertThat(file.samplesView()).containsExactly(sample);
    }

    @Test
    void addAnalysisRequest_rejects_when_status_not_OPEN() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        forceStatus(file, LabFileStatus.PUBLISHED);

        assertThatThrownBy(
                        () ->
                                file.addAnalysisRequest(
                                        new NewAnalysisRequest("01ANALYSIS", "GLUC"), FIXED))
                .isInstanceOf(LabFileNotEditableException.class);
    }

    @Test
    void removeAnalysisRequest_blocks_when_a_sample_references_the_analysis() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        var ar = file.addAnalysisRequest(new NewAnalysisRequest("01ANALYSIS", "GLUC"), FIXED);
        var sample = InLabSample.create("01ANALYSIS", "01RECIPIENT", FIXED.instant(), "01USER");
        sample.attachToFile(file, DAILY);
        file.addSample(sample, FIXED);

        var arId = new AnalysisRequestId(ar.rawId());

        assertThatThrownBy(() -> file.removeAnalysisRequest(arId, FIXED))
                .isInstanceOf(AnalysisHasSamplesException.class)
                .hasMessageContaining("GLUC");
        assertThat(file.analysesView()).hasSize(1); // not removed
    }

    @Test
    void removeAnalysisRequest_throws_when_request_id_unknown() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        var unknownId = AnalysisRequestId.next();

        assertThatThrownBy(() -> file.removeAnalysisRequest(unknownId, FIXED))
                .isInstanceOf(AnalysisRequestNotFoundException.class);
    }

    @Test
    void removeAnalysisRequest_succeeds_when_no_sample_references_it() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        var ar = file.addAnalysisRequest(new NewAnalysisRequest("01ANALYSIS", "GLUC"), FIXED);
        var arId = new AnalysisRequestId(ar.rawId());

        file.removeAnalysisRequest(arId, FIXED);

        assertThat(file.analysesView()).isEmpty();
    }

    @Test
    void removeAnalysisRequest_rejects_when_status_not_OPEN() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        var ar = file.addAnalysisRequest(new NewAnalysisRequest("01ANALYSIS", "GLUC"), FIXED);
        forceStatus(file, LabFileStatus.AWAITING_RESULTS);

        var arId = new AnalysisRequestId(ar.rawId());

        assertThatThrownBy(() -> file.removeAnalysisRequest(arId, FIXED))
                .isInstanceOf(LabFileNotEditableException.class);
    }

    @Test
    void update_with_null_urgent_treated_as_no_change() {
        var openCmd =
                new NewLabFile(
                        "01PATIENT",
                        null,
                        null,
                        true,
                        List.of("GLUC"),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        var file = LabFile.open(openCmd, REG, "01CREATOR", FIXED);
        var laterClock = Clock.fixed(Instant.parse("2026-05-07T09:00:00Z"), ZoneOffset.UTC);

        file.update(
                new UpdateLabFile(
                        null, "01NEWPRESCRIBER", null, null, null, null, null, null, null),
                laterClock);

        assertThat(file.isUrgent()).isTrue();
        assertThat(file.getPrescriberId()).isEqualTo("01NEWPRESCRIBER");
        assertThat(file.getPreleveurId()).isNull();
        assertThat(file.getUpdatedAt()).isEqualTo(laterClock.instant());
    }

    @Test
    void update_clears_urgent_when_explicit_false() {
        var openCmd =
                new NewLabFile(
                        "01PATIENT",
                        null,
                        null,
                        true,
                        List.of("GLUC"),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        var file = LabFile.open(openCmd, REG, "01CREATOR", FIXED);

        file.update(
                new UpdateLabFile(false, null, null, null, null, null, null, null, null), FIXED);

        assertThat(file.isUrgent()).isFalse();
    }

    @Test
    void update_rejects_when_status_not_OPEN() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        forceStatus(file, LabFileStatus.BIOLOGICALLY_VALIDATED);

        assertThatThrownBy(
                        () ->
                                file.update(
                                        new UpdateLabFile(
                                                true, null, null, null, null, null, null, null,
                                                null),
                                        FIXED))
                .isInstanceOf(LabFileNotEditableException.class);
    }

    @Test
    void applyDiscount_rejects_negative_value() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        assertThatThrownBy(() -> file.applyDiscount(DiscountKind.AMOUNT, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void applyDiscount_rejects_percent_above_one() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        assertThatThrownBy(() -> file.applyDiscount(DiscountKind.PERCENT, new BigDecimal("1.5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("<= 1");
    }

    @Test
    void applyDiscount_rejects_kind_without_value() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        assertThatThrownBy(() -> file.applyDiscount(DiscountKind.AMOUNT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both null or both set");
    }

    @Test
    void applyDiscount_rejects_value_without_kind() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        assertThatThrownBy(() -> file.applyDiscount(null, new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both null or both set");
    }

    @Test
    void applyDiscount_sets_amount_discount_when_valid() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        file.applyDiscount(DiscountKind.AMOUNT, new BigDecimal("250"));
        assertThat(file.getDiscountKind()).isEqualTo(DiscountKind.AMOUNT);
        assertThat(file.getDiscountValue()).isEqualByComparingTo(new BigDecimal("250"));
    }

    @Test
    void applyDiscount_clears_when_both_null() {
        var file = LabFile.open(newCmd(), REG, "01CREATOR", FIXED);
        file.applyDiscount(DiscountKind.PERCENT, new BigDecimal("0.2"));
        file.applyDiscount(null, null);
        assertThat(file.getDiscountKind()).isNull();
        assertThat(file.getDiscountValue()).isNull();
    }

    @Test
    void open_rejects_negative_home_collection_fee() {
        var cmd =
                new NewLabFile(
                        "01PATIENT",
                        null,
                        null,
                        false,
                        List.of("GLUC"),
                        null,
                        null,
                        new BigDecimal("-1"),
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        assertThatThrownBy(() -> LabFile.open(cmd, REG, "01CREATOR", FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void open_rejects_unpaired_discount_kind_without_value() {
        var cmd =
                new NewLabFile(
                        "01PATIENT",
                        null,
                        null,
                        false,
                        List.of("GLUC"),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DiscountKind.AMOUNT,
                        null,
                        null);
        assertThatThrownBy(() -> LabFile.open(cmd, REG, "01CREATOR", FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both null or both set");
    }

    /**
     * Reflection-based status mutation to simulate downstream lifecycle transitions (1.5+) without
     * leaking a public mutator on the aggregate. The 1.4 contract only writes {@code OPEN}.
     */
    private static void forceStatus(LabFile file, LabFileStatus status) {
        try {
            var f = LabFile.class.getDeclaredField("status");
            f.setAccessible(true);
            f.set(file, status);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
