package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.DisciplineId;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.DisciplineNotFoundException;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

class DefaultDisciplineApiTest {

    private final DisciplineRepository repo = mock(DisciplineRepository.class);
    private final DefaultDisciplineApi api = new DefaultDisciplineApi(repo);

    @Test
    void findById_returns_summary_when_present() {
        var d = Discipline.create("BIO", "Biochimie", null, DisciplinePrefix.BIOCHIMIE);
        when(repo.findById(d.rawId())).thenReturn(Optional.of(d));
        var s = api.findById(d.id());
        assertThat(s).isPresent();
        assertThat(s.get().code()).isEqualTo("BIO");
        assertThat(s.get().active()).isTrue();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(DisciplineId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(DisciplineId.next()))
                .isInstanceOf(DisciplineNotFoundException.class);
    }

    @Test
    void existsById_delegates_to_repo() {
        var id = DisciplineId.next();
        when(repo.existsById(id.value())).thenReturn(true);
        assertThat(api.existsById(id)).isTrue();
    }
}
