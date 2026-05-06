package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.RecipientId;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.Recipient;
import org.sparkcorporation.lims.catalog.domain.RecipientNotFoundException;

class DefaultRecipientApiTest {

    private final RecipientRepository repo = mock(RecipientRepository.class);
    private final DefaultRecipientApi api = new DefaultRecipientApi(repo);

    @Test
    void findById_returns_summary_when_present() {
        var r = Recipient.create("TUBE_X", "Tube X", null, null, null, null);
        when(repo.findById(r.rawId())).thenReturn(Optional.of(r));
        var s = api.findById(r.id());
        assertThat(s).isPresent();
        assertThat(s.get().code()).isEqualTo("TUBE_X");
        assertThat(s.get().active()).isTrue();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(RecipientId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(RecipientId.next()))
                .isInstanceOf(RecipientNotFoundException.class);
    }

    @Test
    void existsById_delegates_to_repo() {
        var id = RecipientId.next();
        when(repo.existsById(id.value())).thenReturn(true);
        assertThat(api.existsById(id)).isTrue();
    }

    @Test
    void summary_embeds_defaultMatrix_snapshot_when_present() {
        var matrix = Matrix.create("SANG", "Sang total", null);
        var r = Recipient.create("TUBE_X", "Tube X", null, "violet", new BigDecimal("5"), matrix);
        when(repo.findById(r.rawId())).thenReturn(Optional.of(r));

        var summary = api.findById(r.id()).orElseThrow();

        assertThat(summary.defaultMatrix()).isNotNull();
        assertThat(summary.defaultMatrix().code()).isEqualTo("SANG");
        assertThat(summary.defaultMatrix().name()).isEqualTo("Sang total");
    }

    @Test
    void summary_has_null_defaultMatrix_when_recipient_has_none() {
        var r = Recipient.create("TUBE_X", "Tube X", null, null, null, null);
        when(repo.findById(r.rawId())).thenReturn(Optional.of(r));

        var summary = api.findById(r.id()).orElseThrow();

        assertThat(summary.defaultMatrix()).isNull();
    }
}
