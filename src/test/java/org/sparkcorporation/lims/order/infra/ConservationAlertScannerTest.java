package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class ConservationAlertScannerTest {

    @Test
    void scan_delegates_to_service() {
        var service = mock(ConservationAlertService.class);
        when(service.sendOverdueDigest()).thenReturn(3);

        new ConservationAlertScanner(service).scan();

        verify(service, times(1)).sendOverdueDigest();
    }

    @Test
    void scan_swallows_service_failure_to_keep_schedule_alive() {
        var service = mock(ConservationAlertService.class);
        when(service.sendOverdueDigest()).thenThrow(new RuntimeException("transient DB error"));

        // Fail-safe: a transient failure must not propagate out of the scheduled method.
        assertThatCode(() -> new ConservationAlertScanner(service).scan())
                .doesNotThrowAnyException();
    }
}
