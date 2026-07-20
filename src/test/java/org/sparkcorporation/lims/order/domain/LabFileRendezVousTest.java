package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;

class LabFileRendezVousTest {

    private static final Instant NOW = Instant.parse("2026-06-22T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String PATIENT = "01HZB0000000000000000PAT01";

    private static LabFile openFile() {
        // NewLabFile(patientId, prescriberId, preleveurId, urgent, analysisCodes, insuranceId,
        //   partnerId, homeCollectionFee, additionalFees, discountKind, discountValue,
        // rendezVousAt)
        var cmd =
                new NewLabFile(
                        PATIENT,
                        null,
                        null,
                        false,
                        List.of(),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        return LabFile.open(cmd, new LabFileRegistrationNumber("LF-260622-0001"), "user-1", CLOCK);
    }

    @Test
    void schedule_sets_rendezvous_and_bumps_updated_at() {
        var f = openFile();
        var when = Instant.parse("2026-06-25T14:30:00Z");
        f.scheduleRendezVous(when, CLOCK);
        assertThat(f.getRendezVousAt()).isEqualTo(when);
        assertThat(f.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void schedule_with_null_clears_rendezvous() {
        var f = openFile();
        f.scheduleRendezVous(Instant.parse("2026-06-25T14:30:00Z"), CLOCK);
        f.scheduleRendezVous(null, CLOCK);
        assertThat(f.getRendezVousAt()).isNull();
    }

    @Test
    void schedule_works_when_not_open() {
        var f = openFile();
        f.markSigned("biologist-1", CLOCK); // status → SIGNED (no longer OPEN)
        var when = Instant.parse("2026-06-26T09:00:00Z");
        f.scheduleRendezVous(when, CLOCK); // must NOT throw LabFileNotEditableException
        assertThat(f.getRendezVousAt()).isEqualTo(when);
    }

    @Test
    void open_carries_creation_rendezvous() {
        var when = Instant.parse("2026-06-25T14:30:00Z");
        var cmd =
                new NewLabFile(
                        PATIENT,
                        null,
                        null,
                        false,
                        List.of(),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        when);
        var f = LabFile.open(cmd, new LabFileRegistrationNumber("LF-260622-0002"), "user-1", CLOCK);
        assertThat(f.getRendezVousAt()).isEqualTo(when);
    }
}
