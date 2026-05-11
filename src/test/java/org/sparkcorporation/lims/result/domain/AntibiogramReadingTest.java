package org.sparkcorporation.lims.result.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.AntibioticSummary;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.order.api.AnalysisRequestId;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.shared.domain.PatientCategory;
import org.sparkcorporation.lims.shared.domain.Sensitivity;
import org.sparkcorporation.lims.shared.domain.Sex;

class AntibiogramReadingTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneOffset.UTC);

    private static Result newResult() {
        var cmd =
                new NewResult(
                        new LabFileId("01KQZ0WPVQA364NMRAJMKRQC84"),
                        new AnalysisRequestId("01KQZ0WPVQA364NMRAJMKRQC85"),
                        new AnalysisId("01KQZ0WPVQA364NMRAJMKRQC86"),
                        "GLU",
                        AnalysisKind.SIMPLE,
                        ResultType.REAL,
                        List.of(),
                        Sex.M,
                        12000,
                        PatientCategory.HOMME,
                        null,
                        null,
                        null,
                        null);
        return Result.create(cmd, FIXED);
    }

    private static AntibioticSummary amx(BigDecimal lo, BigDecimal hi) {
        return new AntibioticSummary(
                new AntibioticId("01KQZ0WPVQA364NMRAJMKRQC99"),
                "AMX",
                "Amoxicilline",
                null,
                "Beta-lactamines",
                new BigDecimal("21.00"),
                lo,
                hi,
                true);
    }

    @Test
    void create_persists_measured_diameter_and_denormalized_sensitivity() {
        Result r = newResult();
        var ab = amx(new BigDecimal("16.00"), new BigDecimal("20.00"));
        var cmd =
                new NewAntibiogramReading(
                        new AntibioticId("01KQZ0WPVQA364NMRAJMKRQC99"),
                        new BigDecimal("25.00"),
                        ab);

        var reading = r.addAntibiogramReading(cmd);

        assertThat(reading.getMeasuredDiameter()).isEqualByComparingTo("25.00");
        assertThat(reading.getInterpretedSensitivity()).isEqualTo(Sensitivity.S);
        assertThat(reading.getPosition()).isEqualTo(0);
        assertThat(r.getAntibiogramReadings()).hasSize(1);
    }

    @Test
    void create_with_R_zone_returns_R_per_AntibiogramInterpreter() {
        Result r = newResult();
        var ab = amx(new BigDecimal("16.00"), new BigDecimal("20.00"));
        var cmd =
                new NewAntibiogramReading(
                        new AntibioticId("01KQZ0WPVQA364NMRAJMKRQC99"),
                        new BigDecimal("10.00"),
                        ab);

        var reading = r.addAntibiogramReading(cmd);

        assertThat(reading.getInterpretedSensitivity()).isEqualTo(Sensitivity.R);
    }

    @Test
    void create_with_S_zone_returns_S() {
        Result r = newResult();
        var ab = amx(new BigDecimal("16.00"), new BigDecimal("20.00"));
        var cmd =
                new NewAntibiogramReading(
                        new AntibioticId("01KQZ0WPVQA364NMRAJMKRQC99"),
                        new BigDecimal("30.00"),
                        ab);

        var reading = r.addAntibiogramReading(cmd);

        assertThat(reading.getInterpretedSensitivity()).isEqualTo(Sensitivity.S);
    }

    @Test
    void create_with_I_zone_returns_I() {
        Result r = newResult();
        var ab = amx(new BigDecimal("16.00"), new BigDecimal("20.00"));
        var cmd =
                new NewAntibiogramReading(
                        new AntibioticId("01KQZ0WPVQA364NMRAJMKRQC99"),
                        new BigDecimal("18.00"),
                        ab);

        var reading = r.addAntibiogramReading(cmd);

        assertThat(reading.getInterpretedSensitivity()).isEqualTo(Sensitivity.I);
    }

    @Test
    void create_with_null_intermediate_band_falls_through_to_reference_diameter() {
        // Realistic case: catalog antibiotics frequently have no intermediate window.
        // AntibiogramInterpreter skips the band check when lo or hi is null and falls back to
        // the reference-diameter comparison: measured < reference → R, otherwise S.
        Result r = newResult();
        var abNoBand = amx(null, null); // reference=21.00, no intermediate band
        var below =
                new NewAntibiogramReading(
                        new AntibioticId("01KQZ0WPVQA364NMRAJMKRQC99"),
                        new BigDecimal("15.00"),
                        abNoBand);
        var above =
                new NewAntibiogramReading(
                        new AntibioticId("01KQZ0WPVQA364NMRAJMKRQC99"),
                        new BigDecimal("25.00"),
                        abNoBand);

        var rBelow = r.addAntibiogramReading(below);
        var rAbove = r.addAntibiogramReading(above);

        assertThat(rBelow.getInterpretedSensitivity()).isEqualTo(Sensitivity.R);
        assertThat(rAbove.getInterpretedSensitivity()).isEqualTo(Sensitivity.S);
    }
}
