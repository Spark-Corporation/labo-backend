package org.sparkcorporation.lims.delivery.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.notification.api.Channel;
import org.sparkcorporation.lims.notification.api.ContextRef;
import org.sparkcorporation.lims.notification.api.ContextType;
import org.sparkcorporation.lims.notification.api.NotificationDelivered;
import org.sparkcorporation.lims.notification.api.NotificationId;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Verifies the {@code NotificationDelivered} → {@code lab_files.emailed_at} stamping listener: an
 * EMAIL delivery for a LAB_FILE context stamps {@code emailed_at}; a WHATSAPP delivery or a QUOTE
 * context does not touch lab files.
 */
@Tag("integration")
@SpringBootTest
class EmailedAtStampingListenerIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    @Autowired private ApplicationEventPublisher events;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate tx;

    @Test
    void emailDeliveryForLabFile_stampsEmailedAt() {
        String labFileId = seedLabFile("PUBLISHED");
        var occurredAt = Instant.parse("2026-05-29T12:00:00Z");

        publishDelivered(
                Channel.EMAIL, new ContextRef(ContextType.LAB_FILE, labFileId), occurredAt);

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(emailedAtOf(labFileId)).isEqualTo(occurredAt));
    }

    @Test
    void whatsAppDeliveryForLabFile_doesNotStampEmailedAt() {
        String labFileId = seedLabFile("PUBLISHED");

        publishDelivered(
                Channel.WHATSAPP,
                new ContextRef(ContextType.LAB_FILE, labFileId),
                Instant.parse("2026-05-29T12:00:00Z"));

        // The listener ignores non-EMAIL channels; give it a moment then assert no stamp.
        assertThat(emailedAtOf(labFileId)).isNull();
    }

    @Test
    void emailDeliveryForQuoteContext_doesNotTouchLabFiles() {
        String labFileId = seedLabFile("PUBLISHED");

        publishDelivered(
                Channel.EMAIL,
                new ContextRef(ContextType.QUOTE, Ulid.generate()),
                Instant.parse("2026-05-29T12:00:00Z"));

        assertThat(emailedAtOf(labFileId)).isNull();
    }

    private void publishDelivered(Channel channel, ContextRef context, Instant occurredAt) {
        // The listener is AFTER_COMMIT — publish inside a committed transaction so it fires.
        tx.executeWithoutResult(
                status ->
                        events.publishEvent(
                                new NotificationDelivered(
                                        new NotificationId(Ulid.generate()),
                                        channel,
                                        context,
                                        occurredAt)));
    }

    private @org.jspecify.annotations.Nullable Instant emailedAtOf(String labFileId) {
        return jdbc.queryForObject(
                "SELECT emailed_at FROM lab_files WHERE id = ?", Instant.class, labFileId);
    }

    @Transactional
    String seedLabFile(String status) {
        String patientId = seedPatient();
        var id = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, ?, now(),"
                        + " now(), 0)",
                id,
                regNum,
                patientId,
                status);
        return id;
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }
}
