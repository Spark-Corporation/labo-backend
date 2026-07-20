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

import org.sparkcorporation.lims.catalog.api.TechniqueCreated;
import org.sparkcorporation.lims.catalog.api.TechniqueDisabled;
import org.sparkcorporation.lims.catalog.api.TechniqueId;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Technique;
import org.sparkcorporation.lims.catalog.domain.TechniqueNotFoundException;
import org.sparkcorporation.lims.catalog.infra.TechniqueRepository;

class TechniqueServiceTest {

    private final TechniqueRepository repo = mock(TechniqueRepository.class);
    private final TechniqueMapper mapper = new TechniqueMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final TechniqueService service = new TechniqueService(repo, mapper, events, clock);

    @Test
    void create_publishes_TechniqueCreated_event() {
        when(repo.existsByCode("NEW")).thenReturn(false);
        when(repo.save(any(Technique.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(new CreateTechniqueRequest("NEW", "New", null));
        verify(events).publishEvent(any(TechniqueCreated.class));
    }

    @Test
    void create_throws_when_code_exists() {
        when(repo.existsByCode("DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateTechniqueRequest("DUP", "Dup", null)))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void get_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(TechniqueId.next()))
                .isInstanceOf(TechniqueNotFoundException.class);
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        TechniqueId.next(), new UpdateTechniqueRequest("X", null)))
                .isInstanceOf(TechniqueNotFoundException.class);
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var t = Technique.create("DEL", "Del", null);
        when(repo.findById(t.rawId())).thenReturn(Optional.of(t));
        service.delete(t.id());
        verify(repo).delete(t);
        verify(events).publishEvent(any(TechniqueDisabled.class));
    }

    /** Spec §7.5 idempotency rule — load-bearing test. */
    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var t = Technique.create("DEL", "Del", null);
        var deletedAtField = Technique.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(t, Instant.now());

        when(repo.findById(t.rawId())).thenReturn(Optional.of(t));
        service.delete(t.id());

        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(TechniqueId.next()))
                .isInstanceOf(TechniqueNotFoundException.class);
    }
}
