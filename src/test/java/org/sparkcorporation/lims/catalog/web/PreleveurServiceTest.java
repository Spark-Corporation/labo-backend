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
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import org.sparkcorporation.lims.catalog.api.Habilitation;
import org.sparkcorporation.lims.catalog.api.IamUserExistenceCheck;
import org.sparkcorporation.lims.catalog.api.PreleveurCreated;
import org.sparkcorporation.lims.catalog.api.PreleveurDisabled;
import org.sparkcorporation.lims.catalog.api.PreleveurId;
import org.sparkcorporation.lims.catalog.api.PreleveurUpdated;
import org.sparkcorporation.lims.catalog.domain.IamUserNotFoundException;
import org.sparkcorporation.lims.catalog.domain.NewPreleveur;
import org.sparkcorporation.lims.catalog.domain.Preleveur;
import org.sparkcorporation.lims.catalog.domain.PreleveurNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdatePreleveur;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeGenerator.EntityType;
import org.sparkcorporation.lims.catalog.infra.PreleveurRepository;

class PreleveurServiceTest {

    private final PreleveurRepository repo = mock(PreleveurRepository.class);
    private final CatalogCodeGenerator codeGenerator = mock(CatalogCodeGenerator.class);
    private final IamUserExistenceCheck iamUserCheck = mock(IamUserExistenceCheck.class);
    private final PreleveurMapper mapper = new PreleveurMapper();
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
    private final PreleveurService service =
            new PreleveurService(repo, codeGenerator, iamUserCheck, mapper, events, clock);

    private static NewPreleveur newCmd(String userId) {
        return new NewPreleveur("Alice", null, null, userId, Set.of(Habilitation.TOUT_LE_MONDE));
    }

    @Test
    void create_publishes_PreleveurCreated_event() {
        when(codeGenerator.next(EntityType.PRELEVEUR)).thenReturn("PHL-000001");
        when(repo.save(any(Preleveur.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(newCmd(null));
        verify(events).publishEvent(any(PreleveurCreated.class));
    }

    @Test
    void create_with_userId_calls_existence_check() {
        when(iamUserCheck.exists("01HZB0000000000000000USER1")).thenReturn(true);
        when(codeGenerator.next(EntityType.PRELEVEUR)).thenReturn("PHL-000002");
        when(repo.save(any(Preleveur.class))).thenAnswer(inv -> inv.getArgument(0));
        service.create(newCmd("01HZB0000000000000000USER1"));
        verify(iamUserCheck).exists("01HZB0000000000000000USER1");
    }

    @Test
    void create_throws_IamUserNotFound_when_userId_missing() {
        when(iamUserCheck.exists("01HZB0000000000000000NOPE0")).thenReturn(false);
        assertThatThrownBy(() -> service.create(newCmd("01HZB0000000000000000NOPE0")))
                .isInstanceOf(IamUserNotFoundException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void update_throws_when_missing() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        PreleveurId.generate(),
                                        new UpdatePreleveur(null, null, null, null, null)))
                .isInstanceOf(PreleveurNotFoundException.class);
    }

    @Test
    void update_throws_IamUserNotFound_when_new_userId_missing() {
        var p =
                Preleveur.create(
                        new NewPreleveur(
                                "Alice", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE)),
                        "PHL-000003",
                        clock);
        when(repo.findById(p.rawId())).thenReturn(Optional.of(p));
        when(iamUserCheck.exists("01HZB0000000000000000NOPE1")).thenReturn(false);
        assertThatThrownBy(
                        () ->
                                service.update(
                                        p.id(),
                                        new UpdatePreleveur(
                                                null,
                                                null,
                                                null,
                                                "01HZB0000000000000000NOPE1",
                                                null)))
                .isInstanceOf(IamUserNotFoundException.class);
    }

    @Test
    void update_publishes_event_when_found() {
        var p =
                Preleveur.create(
                        new NewPreleveur(
                                "Alice", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE)),
                        "PHL-000004",
                        clock);
        when(repo.findById(p.rawId())).thenReturn(Optional.of(p));
        service.update(p.id(), new UpdatePreleveur("Alice 2", null, null, null, null));
        verify(events).publishEvent(any(PreleveurUpdated.class));
    }

    @Test
    void delete_active_calls_repo_delete_and_publishes_event() {
        var p =
                Preleveur.create(
                        new NewPreleveur(
                                "Alice", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE)),
                        "PHL-000005",
                        clock);
        when(repo.findById(p.rawId())).thenReturn(Optional.of(p));
        service.delete(p.id());
        verify(repo).delete(p);
        verify(events).publishEvent(any(PreleveurDisabled.class));
    }

    @Test
    void delete_already_deleted_is_no_op() throws Exception {
        var p =
                Preleveur.create(
                        new NewPreleveur(
                                "Alice", null, null, null, Set.of(Habilitation.TOUT_LE_MONDE)),
                        "PHL-000006",
                        clock);
        var deletedAtField = Preleveur.class.getDeclaredField("deletedAt");
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
        assertThatThrownBy(() -> service.delete(PreleveurId.generate()))
                .isInstanceOf(PreleveurNotFoundException.class);
    }
}
