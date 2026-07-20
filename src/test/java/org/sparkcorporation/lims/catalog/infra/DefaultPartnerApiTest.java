package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
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

import org.sparkcorporation.lims.catalog.api.PartnerCreated;
import org.sparkcorporation.lims.catalog.api.PartnerDisabled;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;
import org.sparkcorporation.lims.catalog.api.PartnerUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.NewPartner;
import org.sparkcorporation.lims.catalog.domain.Partner;
import org.sparkcorporation.lims.catalog.domain.PartnerNotFoundException;
import org.sparkcorporation.lims.catalog.domain.UpdatePartner;

class DefaultPartnerApiTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-12T10:00:00Z"), ZoneOffset.UTC);

    private final PartnerRepository repo = mock(PartnerRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private final DefaultPartnerApi api = new DefaultPartnerApi(repo, FIXED, events);

    private static NewPartner sampleReq() {
        return new NewPartner(
                "ACME",
                "ACME Corp",
                PartnerKind.SOCIETY,
                "hr@acme.tg",
                "+228",
                new BigDecimal("400.00"));
    }

    @Test
    void create_persists_and_publishes_event() {
        when(repo.existsByCode("ACME")).thenReturn(false);
        when(repo.save(any(Partner.class))).thenAnswer(inv -> inv.getArgument(0));

        var summary = api.create(sampleReq());

        assertThat(summary.code()).isEqualTo("ACME");
        assertThat(summary.kind()).isEqualTo(PartnerKind.SOCIETY);
        assertThat(summary.bRate()).isEqualByComparingTo("400.00");
        assertThat(summary.disabled()).isFalse();
        verify(events).publishEvent(any(PartnerCreated.class));
    }

    @Test
    void create_with_duplicate_code_throws() {
        when(repo.existsByCode("ACME")).thenReturn(true);

        assertThatThrownBy(() -> api.create(sampleReq()))
                .isInstanceOf(CodeAlreadyExistsException.class);

        verify(repo, never()).save(any());
    }

    @Test
    void update_returns_404_when_missing() {
        var id = PartnerId.generate();
        when(repo.findById(id.value())).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> api.update(id, new UpdatePartner("renamed", null, null, null, null)))
                .isInstanceOf(PartnerNotFoundException.class);
    }

    @Test
    void update_persists_and_publishes_event() {
        var partner = Partner.create(sampleReq(), FIXED);
        when(repo.findById(partner.rawId())).thenReturn(Optional.of(partner));

        var summary =
                api.update(
                        partner.id(),
                        new UpdatePartner("Updated", PartnerKind.HEALTH_CENTER, null, null, null));

        assertThat(summary.name()).isEqualTo("Updated");
        assertThat(summary.kind()).isEqualTo(PartnerKind.HEALTH_CENTER);
        verify(events).publishEvent(any(PartnerUpdated.class));
    }

    @Test
    void disable_marks_disabled_and_publishes_event() {
        var partner = Partner.create(sampleReq(), FIXED);
        when(repo.findById(partner.rawId())).thenReturn(Optional.of(partner));

        api.disable(partner.id());

        assertThat(partner.isDisabled()).isTrue();
        verify(events).publishEvent(any(PartnerDisabled.class));
    }
}
