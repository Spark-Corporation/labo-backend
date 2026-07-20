package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import dev.sparksam.securedx.core.signing.ActionSigner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.sparkcorporation.lims.order.api.SampleId;
import org.sparkcorporation.lims.order.domain.InLabSample;
import org.sparkcorporation.lims.order.domain.Sample;
import org.sparkcorporation.lims.order.domain.SampleNotFoundException;

class MarkSampleDestroyedServiceTest {

    private static final String SID = "01HZB0000000000000000SMP01";
    private static final String ANALYSIS = "01HZB0000000000000000ANA01";
    private static final String RECIPIENT = "01HZB0000000000000000REC01";
    private static final String COLLECTOR = "01HZB0000000000000000STF01";
    private static final String ACTOR = "01HZB0000000000000000STF09";
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T09:00:00Z"), ZoneOffset.UTC);

    private Sample sample() {
        return InLabSample.create(
                ANALYSIS, RECIPIENT, Instant.parse("2026-06-01T08:00:00Z"), COLLECTOR);
    }

    @Test
    void marks_destroyed_and_signs_without_null_portoir_metadata() {
        var repo = mock(SampleRepository.class);
        var signer = mock(ActionSigner.class);
        Sample sample = sample(); // no conservation snapshot → portoir null
        when(repo.findById(SID)).thenReturn(Optional.of(sample));
        var svc = new MarkSampleDestroyedService(repo, signer, clock);

        svc.markDestroyed(new SampleId(SID), ACTOR);

        assertThat(sample.getDestroyedAt()).isEqualTo(clock.instant());
        assertThat(sample.getDestroyedByUserId()).isEqualTo(ACTOR);
        @SuppressWarnings("unchecked")
        var metadata = ArgumentCaptor.forClass(Map.class);
        verify(signer).sign(eq("sample.destroy"), eq(SID), metadata.capture());
        // A null portoir must NOT leak as the string "null" into the audit record.
        assertThat(metadata.getValue()).doesNotContainValue("null").doesNotContainKey("portoir");
    }

    @Test
    void signs_with_portoir_metadata_when_present() {
        var repo = mock(SampleRepository.class);
        var signer = mock(ActionSigner.class);
        Sample sample = sample();
        sample.snapshotConservation("P07", 14);
        when(repo.findById(SID)).thenReturn(Optional.of(sample));
        var svc = new MarkSampleDestroyedService(repo, signer, clock);

        svc.markDestroyed(new SampleId(SID), ACTOR);

        @SuppressWarnings("unchecked")
        var metadata = ArgumentCaptor.forClass(Map.class);
        verify(signer).sign(eq("sample.destroy"), eq(SID), metadata.capture());
        assertThat(metadata.getValue()).containsEntry("portoir", "P07");
    }

    @Test
    void idempotent_second_call_does_not_resign() {
        var repo = mock(SampleRepository.class);
        var signer = mock(ActionSigner.class);
        Sample sample = sample();
        sample.markDestroyed(Instant.parse("2026-06-20T00:00:00Z"), ACTOR);
        when(repo.findById(SID)).thenReturn(Optional.of(sample));
        var svc = new MarkSampleDestroyedService(repo, signer, clock);

        svc.markDestroyed(new SampleId(SID), ACTOR);

        verifyNoInteractions(signer);
    }

    @Test
    void unknown_sample_throws_404_type() {
        var repo = mock(SampleRepository.class);
        when(repo.findById(SID)).thenReturn(Optional.empty());
        var svc = new MarkSampleDestroyedService(repo, mock(ActionSigner.class), clock);

        assertThatThrownBy(() -> svc.markDestroyed(new SampleId(SID), ACTOR))
                .isInstanceOf(SampleNotFoundException.class);
    }
}
