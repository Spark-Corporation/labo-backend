package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.catalog.api.PrescriberCreated;
import org.sparkcorporation.lims.catalog.api.PrescriberDisabled;
import org.sparkcorporation.lims.catalog.api.PrescriberId;
import org.sparkcorporation.lims.catalog.api.PrescriberUpdated;
import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.domain.NewPrescriber;
import org.sparkcorporation.lims.catalog.domain.NewSpecialty;
import org.sparkcorporation.lims.catalog.domain.Prescriber;
import org.sparkcorporation.lims.catalog.domain.PrescriberNotFoundException;
import org.sparkcorporation.lims.catalog.domain.Specialty;
import org.sparkcorporation.lims.catalog.domain.SpecialtyNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdatePrescriber;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator.EntityType;
import org.sparkcorporation.lims.catalog.infra.PrescriberRepository;
import org.sparkcorporation.lims.catalog.infra.SpecialtyRepository;

class PrescriberServiceTest {

    private final PrescriberRepository repo = mock(PrescriberRepository.class);
    private final SpecialtyRepository specialtyRepo = mock(SpecialtyRepository.class);
    private final CatalogCodeGenerator codeGenerator = mock(CatalogCodeGenerator.class);
    private final PrescriberMapper mapper = new PrescriberMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final PrescriberService service =
            new PrescriberService(repo, specialtyRepo, codeGenerator, mapper, events, clock);

    @Test
    void create_publishes_PrescriberCreated_event_with_no_specialty() {
        when(codeGenerator.next(EntityType.PRESCRIBER)).thenReturn("PRS-000001");
        when(repo.save(any(Prescriber.class))).thenAnswer(inv -> inv.getArgument(0));
        var cmd = new NewPrescriber("Dr.", "Alice", null, null, null, null, null);
        service.create(cmd);
        verify(events).publishEvent(any(PrescriberCreated.class));
    }

    @Test
    void create_resolves_specialty_when_specialtyId_provided() {
        var specialty = Specialty.create(new NewSpecialty("CARDIO", "Cardiologie", null), clock);
        when(specialtyRepo.findActiveById(specialty.rawId())).thenReturn(Optional.of(specialty));
        when(codeGenerator.next(EntityType.PRESCRIBER)).thenReturn("PRS-000002");
        when(repo.save(any(Prescriber.class))).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new NewPrescriber(null, "Bob", specialty.id(), null, null, null, null);
        var resp = service.create(cmd);

        org.assertj.core.api.Assertions.assertThat(resp.specialty()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(resp.specialty().code()).isEqualTo("CARDIO");
    }

    @Test
    void create_throws_when_specialty_missing() {
        when(specialtyRepo.findActiveById(any())).thenReturn(Optional.empty());
        when(codeGenerator.next(EntityType.PRESCRIBER)).thenReturn("PRS-000003");
        var cmd = new NewPrescriber(null, "Bob", SpecialtyId.generate(), null, null, null, null);
        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(SpecialtyNotFoundException.class);
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        var cmd = new UpdatePrescriber(null, "Updated", null, null, null, null, null);
        assertThatThrownBy(() -> service.update(PrescriberId.generate(), cmd))
                .isInstanceOf(PrescriberNotFoundException.class);
    }

    @Test
    void update_resolves_new_specialty_when_specialtyId_changes() {
        var p =
                Prescriber.create(
                        new NewPrescriber(null, "Bob", null, null, null, null, null),
                        "PRS-000004",
                        null,
                        clock);
        var newSpecialty = Specialty.create(new NewSpecialty("ONCO", "Oncologie", null), clock);
        when(repo.findById(p.rawId())).thenReturn(Optional.of(p));
        when(specialtyRepo.findActiveById(newSpecialty.rawId()))
                .thenReturn(Optional.of(newSpecialty));

        service.update(
                p.id(),
                new UpdatePrescriber(null, null, newSpecialty.id(), null, null, null, null));

        verify(events).publishEvent(any(PrescriberUpdated.class));
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var p =
                Prescriber.create(
                        new NewPrescriber(null, "Bob", null, null, null, null, null),
                        "PRS-000005",
                        null,
                        clock);
        when(repo.findById(p.rawId())).thenReturn(Optional.of(p));
        service.delete(p.id());
        verify(repo).delete(p);
        verify(events).publishEvent(any(PrescriberDisabled.class));
    }

    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var p =
                Prescriber.create(
                        new NewPrescriber(null, "Bob", null, null, null, null, null),
                        "PRS-000006",
                        null,
                        clock);
        var deletedAtField = Prescriber.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(p, Instant.now());

        when(repo.findById(p.rawId())).thenReturn(Optional.of(p));
        service.delete(p.id());

        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(PrescriberId.generate()))
                .isInstanceOf(PrescriberNotFoundException.class);
    }
}
