package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.TechniqueId;
import org.sparkcorporation.lims.catalog.domain.Technique;
import org.sparkcorporation.lims.catalog.domain.TechniqueNotFoundException;

class DefaultTechniqueApiTest {

    private final TechniqueRepository repo = mock(TechniqueRepository.class);
    private final DefaultTechniqueApi api = new DefaultTechniqueApi(repo);

    @Test
    void findById_returns_summary_when_present() {
        var t = Technique.create("ELISA", "ELISA", null);
        when(repo.findById(t.rawId())).thenReturn(Optional.of(t));
        var s = api.findById(t.id());
        assertThat(s).isPresent();
        assertThat(s.get().code()).isEqualTo("ELISA");
        assertThat(s.get().active()).isTrue();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(TechniqueId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(TechniqueId.next()))
                .isInstanceOf(TechniqueNotFoundException.class);
    }

    @Test
    void existsById_delegates_to_repo() {
        var id = TechniqueId.next();
        when(repo.existsById(id.value())).thenReturn(true);
        assertThat(api.existsById(id)).isTrue();
    }
}
