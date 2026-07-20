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

import org.sparkcorporation.lims.catalog.api.DisciplineCreated;
import org.sparkcorporation.lims.catalog.api.DisciplineDisabled;
import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.DisciplineNotFoundException;
import org.sparkcorporation.lims.catalog.infra.DisciplineRepository;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

class DisciplineServiceTest {

    private final DisciplineRepository repo = mock(DisciplineRepository.class);
    private final DisciplineMapper mapper = new DisciplineMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final DisciplineService service = new DisciplineService(repo, mapper, events, clock);

    @Test
    void create_publishes_DisciplineCreated_event() {
        when(repo.existsByCode("NEW")).thenReturn(false);
        when(repo.save(any(Discipline.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(new CreateDisciplineRequest("NEW", "New", null, DisciplinePrefix.BIOCHIMIE));
        verify(events).publishEvent(any(DisciplineCreated.class));
    }

    @Test
    void create_throws_when_code_exists() {
        when(repo.existsByCode("DUP")).thenReturn(true);
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateDisciplineRequest(
                                                "DUP", "Dup", null, DisciplinePrefix.BIOCHIMIE)))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void get_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(DisciplineId.next()))
                .isInstanceOf(DisciplineNotFoundException.class);
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        DisciplineId.next(),
                                        new UpdateDisciplineRequest("X", null)))
                .isInstanceOf(DisciplineNotFoundException.class);
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var d = Discipline.create("DEL", "Del", null, DisciplinePrefix.BIOCHIMIE);
        when(repo.findById(d.rawId())).thenReturn(Optional.of(d));
        service.delete(d.id());
        verify(repo).delete(d);
        verify(events).publishEvent(any(DisciplineDisabled.class));
    }

    /** Spec §7.5 idempotency rule — load-bearing test. */
    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var d = Discipline.create("DEL", "Del", null, DisciplinePrefix.BIOCHIMIE);
        var deletedAtField = Discipline.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(d, Instant.now());

        when(repo.findById(d.rawId())).thenReturn(Optional.of(d));
        service.delete(d.id());

        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(DisciplineId.next()))
                .isInstanceOf(DisciplineNotFoundException.class);
    }
}
