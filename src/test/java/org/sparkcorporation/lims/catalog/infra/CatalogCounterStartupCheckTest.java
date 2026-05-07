package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Unit tests for {@link CatalogCounterStartupCheck}: fails fast at boot when either counter row is
 * missing; no-op when both rows are present.
 */
@ExtendWith(MockitoExtension.class)
class CatalogCounterStartupCheckTest {

    @Mock CatalogCodeCounterRepository counters;

    @Test
    void throws_when_PRESCRIBER_row_missing() {
        when(counters.findById("PRESCRIBER")).thenReturn(Optional.empty());

        var check = new CatalogCounterStartupCheck(counters);

        assertThatThrownBy(() -> check.onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRESCRIBER");
    }

    @Test
    void throws_when_PRELEVEUR_row_missing() {
        when(counters.findById("PRESCRIBER"))
                .thenReturn(Optional.of(mock(CatalogCodeCounter.class)));
        when(counters.findById("PRELEVEUR")).thenReturn(Optional.empty());

        var check = new CatalogCounterStartupCheck(counters);

        assertThatThrownBy(() -> check.onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRELEVEUR");
    }

    @Test
    void passes_when_both_rows_present() {
        when(counters.findById("PRESCRIBER"))
                .thenReturn(Optional.of(mock(CatalogCodeCounter.class)));
        when(counters.findById("PRELEVEUR"))
                .thenReturn(Optional.of(mock(CatalogCodeCounter.class)));

        var check = new CatalogCounterStartupCheck(counters);

        assertThatCode(() -> check.onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .doesNotThrowAnyException();
    }
}
