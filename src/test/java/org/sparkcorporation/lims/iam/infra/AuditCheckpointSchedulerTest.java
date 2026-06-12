package org.sparkcorporation.lims.iam.infra;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import dev.sparksam.securedx.core.signing.AuditCheckpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditCheckpointSchedulerTest {

    @Mock private AuditCheckpointService service;

    @InjectMocks private AuditCheckpointScheduler scheduler;

    @Test
    void run_delegates_to_create_checkpoint() {
        when(service.createCheckpoint())
                .thenReturn(
                        new AuditCheckpoint(
                                "cp-1",
                                7L,
                                Instant.parse("2026-06-10T02:00:00Z"),
                                Map.of(),
                                "root",
                                "sig"));

        scheduler.run();

        verify(service, only()).createCheckpoint();
    }

    @Test
    void run_swallows_service_failures() {
        when(service.createCheckpoint()).thenThrow(new IllegalStateException("boom"));

        assertThatCode(scheduler::run).doesNotThrowAnyException();
    }
}
