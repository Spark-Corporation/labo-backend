package org.sparkcorporation.lims.patient.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class PatientApiSurfaceTest {

    @Test
    void summary_exposes_insured_flag() {
        RecordComponent[] components = PatientSummary.class.getRecordComponents();
        boolean hasInsured =
                Arrays.stream(components)
                        .anyMatch(
                                rc ->
                                        rc.getName().equals("insured")
                                                && rc.getType().equals(boolean.class));
        assertThat(hasInsured)
                .as(
                        "PatientSummary must expose a primitive boolean 'insured' field at position"
                                + " 8 — 1.5.b amendment")
                .isTrue();
    }
}
