package org.sparkcorporation.lims.order.infra;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.EmailRequest;
import org.sparkcorporation.lims.notification.api.NotificationService;
import org.sparkcorporation.lims.order.domain.Sample;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Transactional unit of the destruction-alert scan (CDC Onglet « Conservation »: "alerte par
 * message aux utilisateurs … le délai est dépassé"). Finds samples past their destruction deadline,
 * sends ONE digest email per run to the configured ops address, and stamps {@code alerted_at} so
 * each sample is alerted exactly once — all in a single transaction so the outbox write and the
 * dedup stamping commit together. A blank recipient skips the push (the in-app queue still serves
 * the same data). The digest body carries only non-PII identifiers (daily-id / sample ULID,
 * portoir, analysis id) per the logging-hygiene rule. Errors propagate to {@link
 * ConservationAlertScanner}, which keeps the schedule alive.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConservationAlertService {

    private static final String SUBJECT = "Échantillons à détruire — délai dépassé";

    private final SampleRepository sampleRepository;
    private final NotificationService notificationService;
    private final ConservationAlertProperties props;
    private final Clock clock;

    /**
     * Sends the overdue-destruction digest and stamps {@code alerted_at} on the included samples.
     *
     * @return the number of samples alerted (0 when none are overdue or the recipient is blank)
     */
    @Transactional
    public int sendOverdueDigest() {
        var now = clock.instant();
        List<Sample> overdue = sampleRepository.findOverdueUnalerted(now);
        if (overdue.isEmpty()) {
            return 0;
        }
        String recipient = props.getAlertRecipient();
        if (recipient == null || recipient.isBlank()) {
            log.warn(
                    "conservation alert: {} sample(s) overdue but"
                            + " lims.conservation.alert-recipient is blank; push skipped",
                    overdue.size());
            return 0;
        }
        String body = buildBody(overdue, now);
        var context = new ContextRef(ContextType.SAMPLE, Ulid.generate()); // per-run batch id
        notificationService.send(new EmailRequest(recipient, SUBJECT, body, null, context));
        overdue.forEach(s -> s.markAlerted(now));
        log.info("conservation alert digest enqueued for {} overdue sample(s)", overdue.size());
        return overdue.size();
    }

    private static String buildBody(List<Sample> overdue, Instant now) {
        // daily-id (or sample ULID fallback) + portoir + analysis id + days-overdue only — NO PII.
        return overdue.stream()
                .map(
                        s ->
                                "- "
                                        + (s.getDailyId() != null
                                                ? s.getDailyId().value()
                                                : s.rawId())
                                        + " | portoir "
                                        + s.getPortoir()
                                        + " | analyse "
                                        + s.getAnalysisId()
                                        + " | "
                                        + ChronoUnit.DAYS.between(s.getDestructionDueAt(), now)
                                        + " j de retard")
                .collect(
                        Collectors.joining(
                                "\n",
                                "Échantillons dont le délai de conservation est dépassé :\n",
                                "\n"));
    }
}
