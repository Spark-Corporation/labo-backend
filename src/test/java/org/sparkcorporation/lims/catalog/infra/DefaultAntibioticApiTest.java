package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.domain.Antibiotic;
import org.sparkcorporation.lims.catalog.domain.AntibioticNotFoundException;

class DefaultAntibioticApiTest {

    private final AntibioticRepository repo = mock(AntibioticRepository.class);
    private final DefaultAntibioticApi api = new DefaultAntibioticApi(repo);

    @Test
    void findById_returns_summary_when_present() {
        var a =
                Antibiotic.create(
                        "AMOX",
                        "Amoxicilline",
                        null,
                        "Beta-lactam",
                        new BigDecimal("18"),
                        null,
                        null);
        when(repo.findById(a.rawId())).thenReturn(Optional.of(a));
        var s = api.findById(a.id());
        assertThat(s).isPresent();
        assertThat(s.get().code()).isEqualTo("AMOX");
        assertThat(s.get().active()).isTrue();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(AntibioticId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(AntibioticId.next()))
                .isInstanceOf(AntibioticNotFoundException.class);
    }

    @Test
    void existsById_delegates_to_repo() {
        var id = AntibioticId.next();
        when(repo.existsById(id.value())).thenReturn(true);
        assertThat(api.existsById(id)).isTrue();
    }
}
