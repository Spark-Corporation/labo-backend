package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.MatrixId;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.MatrixNotFoundException;

class DefaultMatrixApiTest {

    private final MatrixRepository repo = mock(MatrixRepository.class);
    private final DefaultMatrixApi api = new DefaultMatrixApi(repo);

    @Test
    void findById_returns_summary_when_present() {
        var m = Matrix.create("SANG", "Sang total", null);
        when(repo.findById(m.rawId())).thenReturn(Optional.of(m));
        var s = api.findById(m.id());
        assertThat(s).isPresent();
        assertThat(s.get().code()).isEqualTo("SANG");
        assertThat(s.get().active()).isTrue();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(MatrixId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(MatrixId.next()))
                .isInstanceOf(MatrixNotFoundException.class);
    }

    @Test
    void existsById_delegates_to_repo() {
        var id = MatrixId.next();
        when(repo.existsById(id.value())).thenReturn(true);
        assertThat(api.existsById(id)).isTrue();
    }
}
