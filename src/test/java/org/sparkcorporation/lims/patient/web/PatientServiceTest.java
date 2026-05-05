package org.sparkcorporation.lims.patient.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.sparkcorporation.lims.patient.api.PatientCreated;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.api.PatientMerged;
import org.sparkcorporation.lims.patient.domain.LabCode;
import org.sparkcorporation.lims.patient.domain.LabCodeGenerator;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PatientMerger;
import org.sparkcorporation.lims.patient.domain.PatientNotFoundException;
import org.sparkcorporation.lims.patient.infra.PatientRepository;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

class PatientServiceTest {

    private final PatientRepository repo = mock(PatientRepository.class);
    private final LabCodeGenerator gen = mock(LabCodeGenerator.class);
    private final PatientMapper mapper = new PatientMapper();
    private final PatientMerger merger = new PatientMerger();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-05T10:00:00Z"), ZoneOffset.UTC);
    private final PatientService service =
            new PatientService(repo, gen, mapper, merger, events, encoder, clock);

    private static CreatePatientRequest req() {
        return new CreatePatientRequest(
                new CreatePatientRequest.NameRequest("DOE", "Jane", null),
                Civility.MADAME,
                Sex.F,
                null,
                null,
                null,
                null,
                null,
                new CreatePatientRequest.ContactRequest(null, null),
                null,
                false,
                null,
                null);
    }

    @Test
    void create_generates_lab_code_saves_and_publishes_event() {
        when(gen.next(LocalDate.of(2026, 5, 5))).thenReturn(new LabCode("LAB-260505-001"));
        when(repo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.create(req());

        assertThat(resp.labCode()).isEqualTo("LAB-260505-001");
        verify(repo).save(any(Patient.class));
        verify(events).publishEvent(any(PatientCreated.class));
    }

    @Test
    void create_hashes_password_when_provided() {
        when(gen.next(any())).thenReturn(new LabCode("LAB-260505-002"));
        when(repo.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encoder.encode("plain-password")).thenReturn("$argon2id$...$hash");

        var withPwd =
                new CreatePatientRequest(
                        req().name(),
                        req().civility(),
                        req().sex(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        req().contact(),
                        null,
                        false,
                        "jane.doe",
                        "plain-password");

        service.create(withPwd);
        verify(encoder).encode("plain-password");
    }

    @Test
    void merge_throws_when_source_missing() {
        var src = PatientId.next();
        var tgt = PatientId.next();
        when(repo.findById(src.value())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.merge(src, tgt))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void merge_throws_when_target_missing() {
        var srcEntity =
                Patient.create(mapper.toCreateCommand(req(), null), new LabCode("LAB-260505-005"));
        var tgt = PatientId.next();
        when(repo.findById(srcEntity.id().value())).thenReturn(Optional.of(srcEntity));
        when(repo.findById(tgt.value())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.merge(srcEntity.id(), tgt))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void merge_publishes_PatientMerged_after_commit() {
        var src =
                Patient.create(mapper.toCreateCommand(req(), null), new LabCode("LAB-260505-003"));
        var target = PatientId.next();
        var targetEntity =
                Patient.create(mapper.toCreateCommand(req(), null), new LabCode("LAB-260505-004"));

        when(repo.findById(src.id().value())).thenReturn(Optional.of(src));
        when(repo.findById(target.value())).thenReturn(Optional.of(targetEntity));

        service.merge(src.id(), target);

        assertThat(src.isMerged()).isTrue();
        verify(events).publishEvent(any(PatientMerged.class));
    }
}
