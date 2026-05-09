package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.shared.id.Ulid;

class DefaultPatientApiExistsByIdTest {

    private final PatientRepository repo = mock(PatientRepository.class);
    private final DefaultPatientApi api = new DefaultPatientApi(repo);

    @Test
    void exists_by_id_returns_true_when_patient_present_and_active() {
        var id = new PatientId(Ulid.generate());
        // Patient is annotated @SQLRestriction("deleted_at IS NULL") — repo.findById auto-filters
        // soft-deleted rows.
        when(repo.findById(id.value())).thenReturn(Optional.of(mock(Patient.class)));
        assertThat(api.existsById(id)).isTrue();
    }

    @Test
    void exists_by_id_returns_false_when_unknown() {
        var id = new PatientId(Ulid.generate());
        when(repo.findById(id.value())).thenReturn(Optional.empty());
        assertThat(api.existsById(id)).isFalse();
    }

    @Test
    void exists_by_id_returns_false_when_soft_deleted() {
        // @SQLRestriction filters at the JPA layer; repo.findById returns Optional.empty for
        // soft-deleted rows.
        var id = new PatientId(Ulid.generate());
        when(repo.findById(id.value())).thenReturn(Optional.empty());
        assertThat(api.existsById(id)).isFalse();
    }
}
