package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.domain.ContactInfo;
import org.sparkcorporation.lims.patient.domain.LabCode;
import org.sparkcorporation.lims.patient.domain.NewPatient;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PatientNotFoundException;
import org.sparkcorporation.lims.patient.domain.PersonName;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

class DefaultPatientApiTest {

    private final PatientRepository repo = mock(PatientRepository.class);
    private final DefaultPatientApi api = new DefaultPatientApi(repo);

    private static Patient sample() {
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
                        false,
                        null,
                        null),
                new LabCode("LAB-260505-001"));
    }

    @Test
    void findById_returns_summary_when_present() {
        Patient p = sample();
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));

        var summary = api.findById(p.id());
        assertThat(summary).isPresent();
        assertThat(summary.get().labCode()).isEqualTo("LAB-260505-001");
        assertThat(summary.get().lastName()).isEqualTo("DOE");
        assertThat(summary.get().firstNames()).isEqualTo("Jane");
        assertThat(summary.get().merged()).isFalse();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(PatientId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(PatientId.next()))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void followMergeChain_returns_target_when_merged() {
        Patient p = sample();
        var target = PatientId.next();
        p.mergeInto(target);
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));
        assertThat(api.followMergeChain(p.id())).isEqualTo(target);
    }

    @Test
    void followMergeChain_returns_self_when_not_merged() {
        Patient p = sample();
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));
        assertThat(api.followMergeChain(p.id())).isEqualTo(p.id());
    }
}
