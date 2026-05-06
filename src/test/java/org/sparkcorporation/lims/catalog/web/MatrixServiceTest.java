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

import org.sparkcorporation.lims.catalog.api.MatrixCreated;
import org.sparkcorporation.lims.catalog.api.MatrixDisabled;
import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.MatrixNotFoundException;
import org.sparkcorporation.lims.catalog.infra.MatrixRepository;

class MatrixServiceTest {

    private final MatrixRepository repo = mock(MatrixRepository.class);
    private final MatrixMapper mapper = new MatrixMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final MatrixService service = new MatrixService(repo, mapper, events, clock);

    @Test
    void create_publishes_MatrixCreated_event() {
        when(repo.existsByCode("NEW")).thenReturn(false);
        when(repo.save(any(Matrix.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(new CreateMatrixRequest("NEW", "New", null));
        verify(events).publishEvent(any(MatrixCreated.class));
    }

    @Test
    void create_throws_when_code_exists() {
        when(repo.existsByCode("DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreateMatrixRequest("DUP", "Dup", null)))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void get_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(MatrixId.next()))
                .isInstanceOf(MatrixNotFoundException.class);
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () -> service.update(MatrixId.next(), new UpdateMatrixRequest("X", null)))
                .isInstanceOf(MatrixNotFoundException.class);
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var m = Matrix.create("DEL", "Del", null);
        when(repo.findById(m.rawId())).thenReturn(Optional.of(m));
        service.delete(m.id());
        verify(repo).delete(m);
        verify(events).publishEvent(any(MatrixDisabled.class));
    }

    /** Spec §7.5 idempotency rule — load-bearing test. */
    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var m = Matrix.create("DEL", "Del", null);
        var deletedAtField = Matrix.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(m, Instant.now());

        when(repo.findById(m.rawId())).thenReturn(Optional.of(m));
        service.delete(m.id());

        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(MatrixId.next()))
                .isInstanceOf(MatrixNotFoundException.class);
    }
}
