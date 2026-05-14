package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

class PatientMergerTest {

    private final PatientMerger merger = new PatientMerger();

    private static Patient sample(String code) {
        return Patient.create(
                new NewPatient(
                        new PersonName("DOE", "Jane", null),
                        Civility.MADAME,
                        Sex.F,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new ContactInfo(null, null),
                        null,
                        null,
                        null,
                        null,
                        null),
                new LabCode(code));
    }

    @Test
    void rejects_self_merge_with_IllegalArgumentException() {
        Patient source = sample("LAB-260505-001");
        // target id == source id
        assertThatThrownBy(() -> merger.validateMerge(source, source.id(), source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    void rejects_already_merged_source_with_IllegalStateException() {
        Patient source = sample("LAB-260505-002");
        Patient target = sample("LAB-260505-003");
        source.mergeInto(target.id());
        Patient newTarget = sample("LAB-260505-004");
        assertThatThrownBy(() -> merger.validateMerge(source, newTarget.id(), newTarget))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already merged");
    }

    @Test
    void valid_merge_passes_without_exception() {
        Patient source = sample("LAB-260505-005");
        Patient target = sample("LAB-260505-006");
        assertThatCode(() -> merger.validateMerge(source, target.id(), target))
                .doesNotThrowAnyException();
    }
}
