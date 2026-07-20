package org.sparkcorporation.lims.patient.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class PatientApiSurfaceTest {

    @Test
    void summary_exposes_insurance_and_partner_snapshots() {
        RecordComponent[] components = PatientSummary.class.getRecordComponents();

        boolean stillHasInsuredField =
                Arrays.stream(components).anyMatch(rc -> rc.getName().equals("insured"));
        assertThat(stillHasInsuredField).as("1.5.b `insured` boolean must be removed").isFalse();

        assertThat(components[7].getName()).isEqualTo("insurance");
        assertThat(components[7].getType()).isEqualTo(PatientSummary.InsuranceSnapshot.class);
        assertThat(components[8].getName()).isEqualTo("partner");
        assertThat(components[8].getType()).isEqualTo(PatientSummary.PartnerSnapshot.class);
    }
}
