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

import org.sparkcorporation.lims.catalog.api.AntibioticCreated;
import org.sparkcorporation.lims.catalog.api.AntibioticDisabled;
import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.domain.Antibiotic;
import org.sparkcorporation.lims.catalog.domain.AntibioticNotFoundException;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.infra.AntibioticRepository;

class AntibioticServiceTest {

    private final AntibioticRepository repo = mock(AntibioticRepository.class);
    private final AntibioticMapper mapper = new AntibioticMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final AntibioticService service = new AntibioticService(repo, mapper, events, clock);

    private static CreateAntibioticRequest createReq(String code) {
        return new CreateAntibioticRequest(
                code, "Drug", null, null, new BigDecimal("18.00"), null, null);
    }

    @Test
    void create_publishes_AntibioticCreated_event() {
        when(repo.existsByCode("NEW")).thenReturn(false);
        when(repo.save(any(Antibiotic.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(createReq("NEW"));
        verify(events).publishEvent(any(AntibioticCreated.class));
    }

    @Test
    void create_throws_when_code_exists() {
        when(repo.existsByCode("DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(createReq("DUP")))
                .isInstanceOf(CodeAlreadyExistsException.class);
    }

    @Test
    void get_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(AntibioticId.next()))
                .isInstanceOf(AntibioticNotFoundException.class);
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        AntibioticId.next(),
                                        new UpdateAntibioticRequest(
                                                "X", null, null, null, null, null)))
                .isInstanceOf(AntibioticNotFoundException.class);
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var a = Antibiotic.create("DEL", "Del", null, null, new BigDecimal("18.00"), null, null);
        when(repo.findById(a.rawId())).thenReturn(Optional.of(a));
        service.delete(a.id());
        verify(repo).delete(a);
        verify(events).publishEvent(any(AntibioticDisabled.class));
    }

    /** Spec §7.5 idempotency rule — load-bearing test. */
    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var a = Antibiotic.create("DEL", "Del", null, null, new BigDecimal("18.00"), null, null);
        var deletedAtField = Antibiotic.class.getDeclaredField("deletedAt");
        deletedAtField.setAccessible(true);
        deletedAtField.set(a, Instant.now());

        when(repo.findById(a.rawId())).thenReturn(Optional.of(a));
        service.delete(a.id());

        verify(repo, never()).delete(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void delete_throws_when_truly_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(AntibioticId.next()))
                .isInstanceOf(AntibioticNotFoundException.class);
    }
}
