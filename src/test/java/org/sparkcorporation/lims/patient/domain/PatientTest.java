package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

class PatientTest {

    private static final LabCode A_CODE = new LabCode("LAB-260505-001");

    private static NewPatient minimalCommand() {
        return new NewPatient(
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
                false,
                null,
                null);
    }

    @Test
    void create_populates_id_labCode_and_required_fields() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        assertThat(p.id()).isInstanceOf(PatientId.class);
        assertThat(p.labCode()).isEqualTo("LAB-260505-001");
        assertThat(p.civility()).isEqualTo(Civility.MADAME);
        assertThat(p.sex()).isEqualTo(Sex.F);
        assertThat(p.isMerged()).isFalse();
        assertThat(p.isDeleted()).isFalse();
    }

    @Test
    void update_changes_only_non_null_fields() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        var change =
                new UpdatePatient(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "infirmière",
                        null,
                        null,
                        null,
                        null,
                        true,
                        null,
                        null);
        p.update(change);
        assertThat(p.profession()).isEqualTo("infirmière");
        assertThat(p.insured()).isTrue();
        // unchanged
        assertThat(p.civility()).isEqualTo(Civility.MADAME);
        assertThat(p.address()).isNull();
    }

    @Test
    void mergeInto_sets_mergedIntoId_once() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        var target = PatientId.next();
        p.mergeInto(target);
        assertThat(p.isMerged()).isTrue();
        assertThat(p.mergedIntoId()).isEqualTo(target.value());
    }

    @Test
    void mergeInto_rejects_double_merge() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        p.mergeInto(PatientId.next());
        assertThatThrownBy(() -> p.mergeInto(PatientId.next()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already merged");
    }
}
