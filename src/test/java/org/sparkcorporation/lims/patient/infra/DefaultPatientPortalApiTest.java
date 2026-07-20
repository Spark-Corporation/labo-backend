package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.domain.Patient;

class DefaultPatientPortalApiTest {

    private final PatientRepository repo = mock(PatientRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final DefaultPatientPortalApi api = new DefaultPatientPortalApi(repo, encoder);

    private static final PatientId ALICE = new PatientId("01HZXAB0000000000000000001");

    @Test
    void returns_patientId_when_login_and_password_match() {
        Patient p = mock(Patient.class);
        when(p.id()).thenReturn(ALICE); // Patient.id() returns PatientId, not String
        when(p.portalPasswordHash()).thenReturn("$2a$hash");
        when(repo.findByPortalLogin("alice")).thenReturn(Optional.of(p));
        when(encoder.matches("secret", "$2a$hash")).thenReturn(true);

        assertThat(api.authenticate("alice", "secret")).contains(ALICE);
    }

    @Test
    void returns_empty_when_password_wrong() {
        Patient p = mock(Patient.class);
        when(p.portalPasswordHash()).thenReturn("$2a$hash");
        when(repo.findByPortalLogin("alice")).thenReturn(Optional.of(p));
        when(encoder.matches("nope", "$2a$hash")).thenReturn(false);

        assertThat(api.authenticate("alice", "nope")).isEmpty();
    }

    @Test
    void returns_empty_when_login_unknown() {
        when(repo.findByPortalLogin("ghost")).thenReturn(Optional.empty());
        assertThat(api.authenticate("ghost", "secret")).isEmpty();
    }
}
