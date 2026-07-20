package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import org.sparkcorporation.lims.catalog.api.InsuranceCreated;
import org.sparkcorporation.lims.catalog.api.InsuranceDisabled;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.InsuranceUpdated;
import org.sparkcorporation.lims.catalog.domain.CodeAlreadyExistsException;
import org.sparkcorporation.lims.catalog.domain.Insurance;
import org.sparkcorporation.lims.catalog.domain.InsuranceNotFoundException;
import org.sparkcorporation.lims.catalog.domain.NewInsurance;
import org.sparkcorporation.lims.catalog.domain.UpdateInsurance;
import org.sparkcorporation.lims.shared.id.Ulid;

class DefaultInsuranceApiTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-12T10:00:00Z"), ZoneOffset.UTC);

    private final InsuranceRepository repo = mock(InsuranceRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private final DefaultInsuranceApi api = new DefaultInsuranceApi(repo, FIXED, events);

    private static NewInsurance sampleReq() {
        return new NewInsurance(
                "CNSS",
                "Caisse Nationale",
                "contact@cnss.tg",
                "+228",
                new BigDecimal("1500.00"),
                new BigDecimal("0.8000"));
    }

    @Test
    void create_persists_and_publishes_event() {
        when(repo.existsByCode("CNSS")).thenReturn(false);
        when(repo.save(any(Insurance.class)))
                .thenAnswer(inv -> inv.getArgument(0)); // identity save

        var summary = api.create(sampleReq());

        assertThat(summary.code()).isEqualTo("CNSS");
        assertThat(summary.bRate()).isEqualByComparingTo("1500.00");
        assertThat(summary.coverageRate()).isEqualByComparingTo("0.8000");
        assertThat(summary.disabled()).isFalse();
        verify(repo).save(any(Insurance.class));
        verify(events).publishEvent(any(InsuranceCreated.class));
    }

    @Test
    void create_with_duplicate_code_throws_and_does_not_publish() {
        when(repo.existsByCode("CNSS")).thenReturn(true);

        assertThatThrownBy(() -> api.create(sampleReq()))
                .isInstanceOf(CodeAlreadyExistsException.class);

        verify(repo, never()).save(any());
        verify(events, never()).publishEvent(any(InsuranceCreated.class));
    }

    @Test
    void update_returns_404_when_missing() {
        var id = InsuranceId.generate();
        when(repo.findById(id.value())).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                api.update(
                                        id,
                                        new UpdateInsurance("new name", null, null, null, null)))
                .isInstanceOf(InsuranceNotFoundException.class);
    }

    @Test
    void update_persists_and_publishes_event() {
        var insurance = Insurance.create(sampleReq(), FIXED);
        when(repo.findById(insurance.rawId())).thenReturn(Optional.of(insurance));

        var summary =
                api.update(
                        insurance.id(),
                        new UpdateInsurance("Updated Name", null, null, null, null));

        assertThat(summary.name()).isEqualTo("Updated Name");
        verify(events).publishEvent(any(InsuranceUpdated.class));
    }

    @Test
    void disable_publishes_event_when_found() {
        var insurance = Insurance.create(sampleReq(), FIXED);
        when(repo.findById(insurance.rawId())).thenReturn(Optional.of(insurance));

        api.disable(insurance.id());

        assertThat(insurance.isDisabled()).isTrue();
        verify(events).publishEvent(any(InsuranceDisabled.class));
    }

    @Test
    void disable_is_idempotent_when_already_filtered() {
        var id = InsuranceId.of(Ulid.generate());
        when(repo.findById(id.value())).thenReturn(Optional.empty());

        api.disable(id);

        verify(events, never()).publishEvent(any(InsuranceDisabled.class));
    }

    @Test
    void exists_delegates_to_repo() {
        var id = InsuranceId.of(Ulid.generate());
        when(repo.existsById(id.value())).thenReturn(true);

        assertThat(api.exists(id)).isTrue();
        verify(repo).existsById(eq(id.value()));
    }
}
