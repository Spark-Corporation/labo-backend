package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.catalog.api.RecipientCreated;
import org.sparkcorporation.lims.catalog.api.RecipientDisabled;
import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.Recipient;
import org.sparkcorporation.lims.catalog.domain.RecipientNotFoundException;
import org.sparkcorporation.lims.catalog.infra.MatrixRepository;
import org.sparkcorporation.lims.catalog.infra.RecipientRepository;

class RecipientServiceTest {

    private final RecipientRepository repo = mock(RecipientRepository.class);
    private final MatrixRepository matrixRepo = mock(MatrixRepository.class);
    private final RecipientMapper mapper = new RecipientMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final RecipientService service =
            new RecipientService(repo, matrixRepo, mapper, events, clock);

    @Test
    void create_publishes_RecipientCreated_event() {
        when(repo.existsByCode("NEW")).thenReturn(false);
        when(repo.save(any(Recipient.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(
                new CreateRecipientRequest("NEW", "New", null, null, new BigDecimal("4.00"), null));
        verify(events).publishEvent(any(RecipientCreated.class));
    }

    @Test
    void create_throws_when_code_exists() {
        when(repo.existsByCode("DUP")).thenReturn(true);
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateRecipientRequest(
                                                "DUP", "Dup", null, null, null, null)))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void get_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(RecipientId.next()))
                .isInstanceOf(RecipientNotFoundException.class);
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        RecipientId.next(),
                                        new UpdateRecipientRequest("X", null, null, null, null)))
                .isInstanceOf(RecipientNotFoundException.class);
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var r = Recipient.create("DEL", "Del", null, null, null, null);
        when(repo.findById(r.rawId())).thenReturn(Optional.of(r));
        service.delete(r.id());
        verify(repo).delete(r);
        verify(events).publishEvent(any(RecipientDisabled.class));
    }

    /** Spec §7.5 idempotency rule — load-bearing test. */
    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var r = Recipient.create("DEL", "Del", null, null, null, null);
        var deletedAtField = Recipient.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(r, Instant.now());

        when(repo.findById(r.rawId())).thenReturn(Optional.of(r));
        service.delete(r.id());

        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(RecipientId.next()))
                .isInstanceOf(RecipientNotFoundException.class);
    }

    @Test
    void create_resolves_default_matrix_when_id_provided() {
        var matrix = Matrix.create("BLOOD", "Whole Blood", null);
        when(repo.existsByCode("EDTA")).thenReturn(false);
        when(matrixRepo.findById(matrix.rawId())).thenReturn(Optional.of(matrix));
        when(repo.save(any(Recipient.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp =
                service.create(
                        new CreateRecipientRequest(
                                "EDTA",
                                "EDTA Tube",
                                null,
                                "purple",
                                new BigDecimal("4.00"),
                                matrix.rawId()));

        // mapped MatrixSnapshot present
        // (verified via response — RecipientMapper handles non-null default matrix)
        org.assertj.core.api.Assertions.assertThat(resp.defaultMatrix()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(resp.defaultMatrix().code()).isEqualTo("BLOOD");
    }
}
