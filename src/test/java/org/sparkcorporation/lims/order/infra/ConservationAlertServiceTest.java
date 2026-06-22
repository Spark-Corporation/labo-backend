package org.sparkcorporation.lims.order.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.notification.api.NotificationService;
import org.sparkcorporation.lims.order.domain.InLabSample;
import org.sparkcorporation.lims.order.domain.Sample;

class ConservationAlertServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-10T07:00:00Z"), ZoneOffset.UTC);

    private Sample overdue() {
        Sample s =
                InLabSample.create(
                        "01HZB0000000000000000ANA01",
                        "01HZB0000000000000000REC01",
                        Instant.parse("2026-06-01T08:00:00Z"),
                        "01HZB0000000000000000STF01");
        s.snapshotConservation("P03", 10); // due 2026-06-11, overdue at scan
        return s;
    }

    @Test
    void sends_one_digest_and_stamps_alerted_at() {
        var repo = mock(SampleRepository.class);
        var notif = mock(NotificationService.class);
        var sample = overdue();
        when(repo.findOverdueUnalerted(clock.instant())).thenReturn(List.of(sample));
        when(notif.send(any())).thenReturn(NotificationId.next());
        var props = new ConservationAlertProperties();
        props.setAlertRecipient("ops@lab.local");
        var service = new ConservationAlertService(repo, notif, props, clock);

        var captor = ArgumentCaptor.forClass(EmailRequest.class);
        int alerted = service.sendOverdueDigest();

        assertThat(alerted).isEqualTo(1);
        verify(notif, times(1)).send(captor.capture());
        assertThat(sample.getAlertedAt()).isEqualTo(clock.instant());
        var email = captor.getValue();
        assertThat(email.recipient()).isEqualTo("ops@lab.local");
        assertThat(email.context().type()).isEqualTo(ContextType.SAMPLE);
        // Logging-hygiene rule is load-bearing: the digest body carries a non-PII identifier
        // (daily-id when present, else the sample ULID) — never a patient name / dob.
        var body = email.body();
        assertThat(body).contains(sample.rawId()).contains("P03");
        assertThat(body).doesNotContainIgnoringCase("dob").doesNotContainIgnoringCase("name=");
    }

    @Test
    void blank_recipient_skips_push_and_leaves_alerted_at_null() {
        var repo = mock(SampleRepository.class);
        var notif = mock(NotificationService.class);
        var sample = overdue();
        when(repo.findOverdueUnalerted(clock.instant())).thenReturn(List.of(sample));
        var props = new ConservationAlertProperties();
        props.setAlertRecipient("");
        var service = new ConservationAlertService(repo, notif, props, clock);

        int alerted = service.sendOverdueDigest();

        assertThat(alerted).isZero();
        verifyNoInteractions(notif);
        assertThat(sample.getAlertedAt()).isNull();
    }

    @Test
    void no_overdue_samples_sends_nothing() {
        var repo = mock(SampleRepository.class);
        var notif = mock(NotificationService.class);
        when(repo.findOverdueUnalerted(clock.instant())).thenReturn(List.of());
        var props = new ConservationAlertProperties();
        props.setAlertRecipient("ops@lab.local");

        int alerted = new ConservationAlertService(repo, notif, props, clock).sendOverdueDigest();

        assertThat(alerted).isZero();
        verifyNoInteractions(notif);
    }
}
