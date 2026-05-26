package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.AntibioticId;
import org.sparkcorporation.lims.catalog.api.AntibioticSummary;
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

    @Test
    void findAllByIds_returns_map_keyed_by_id_omitting_missing() {
        var a =
                Antibiotic.create(
                        "AMOX",
                        "Amoxicilline",
                        null,
                        "Beta-lactam",
                        new BigDecimal("18"),
                        null,
                        null);
        var presentId = a.id();
        var missingId = AntibioticId.next();
        when(repo.findAllById(List.of(presentId.value(), missingId.value())))
                .thenReturn(List.of(a));

        Map<AntibioticId, AntibioticSummary> result =
                api.findAllByIds(new LinkedHashSet<>(List.of(presentId, missingId)));

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(presentId);
        assertThat(result).doesNotContainKey(missingId);
        assertThat(result.get(presentId).code()).isEqualTo("AMOX");
    }

    @Test
    void findAllByIds_emptyInput_returnsEmptyMap_withoutQuery() {
        var result = api.findAllByIds(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(repo);
    }
}
