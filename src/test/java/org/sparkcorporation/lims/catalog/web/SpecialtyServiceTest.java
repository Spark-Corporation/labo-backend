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

import org.sparkcorporation.lims.catalog.api.SpecialtyCreated;
import org.sparkcorporation.lims.catalog.api.SpecialtyDisabled;
import org.sparkcorporation.lims.catalog.api.SpecialtyId;
import org.sparkcorporation.lims.catalog.api.SpecialtyUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.NewSpecialty;
import org.sparkcorporation.lims.catalog.domain.Specialty;
import org.sparkcorporation.lims.catalog.domain.SpecialtyNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdateSpecialty;
import org.sparkcorporation.lims.catalog.infra.SpecialtyRepository;

class SpecialtyServiceTest {

    private final SpecialtyRepository repo = mock(SpecialtyRepository.class);
    private final SpecialtyMapper mapper = new SpecialtyMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final SpecialtyService service = new SpecialtyService(repo, mapper, events, clock);

    @Test
    void create_publishes_SpecialtyCreated_event() {
        when(repo.findByCode("CARDIO")).thenReturn(Optional.empty());
        when(repo.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(new NewSpecialty("CARDIO", "Cardiologie", null));
        verify(events).publishEvent(any(SpecialtyCreated.class));
    }

    @Test
    void create_throws_when_code_exists() {
        var existing = Specialty.create(new NewSpecialty("DUP", "Existing", null), clock);
        when(repo.findByCode("DUP")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.create(new NewSpecialty("DUP", "Dup", null)))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void update_publishes_SpecialtyUpdated_event_and_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        SpecialtyId.generate(), new UpdateSpecialty("X", null)))
                .isInstanceOf(SpecialtyNotFoundException.class);
    }

    @Test
    void update_loads_and_publishes_event_when_found() {
        var s = Specialty.create(new NewSpecialty("CARDIO", "Cardiologie", null), clock);
        when(repo.findById(s.rawId())).thenReturn(Optional.of(s));
        service.update(s.id(), new UpdateSpecialty("Cardiologie 2", null));
        verify(events).publishEvent(any(SpecialtyUpdated.class));
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var s = Specialty.create(new NewSpecialty("DEL", "Del", null), clock);
        when(repo.findById(s.rawId())).thenReturn(Optional.of(s));
        service.delete(s.id());
        verify(repo).delete(s);
        verify(events).publishEvent(any(SpecialtyDisabled.class));
    }

    /** Spec §7.5 idempotency rule. */
    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var s = Specialty.create(new NewSpecialty("DEL", "Del", null), clock);
        var deletedAtField = Specialty.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(s, Instant.now());

        when(repo.findById(s.rawId())).thenReturn(Optional.of(s));
        service.delete(s.id());

        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(SpecialtyId.generate()))
                .isInstanceOf(SpecialtyNotFoundException.class);
    }
}
