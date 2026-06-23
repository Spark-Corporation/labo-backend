package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.api.LabFileStatus;

class LabFileMarkSignedTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-28T14:10:00Z"), ZoneOffset.UTC);
    private static final LabFileRegistrationNumber REG =
            new LabFileRegistrationNumber("LF-260528-0001");
    private static final String SIGNER = "01SIGNERAAAAAAAAAAAAAAAAAA";

    @Test
    void mark_signed_transitions_to_signed_and_records_signer() {
        LabFile f = newOpenLabFile();

        f.markSigned(SIGNER, FIXED);

        assertThat(f.getStatus()).isEqualTo(LabFileStatus.SIGNED);
        assertThat(f.getSignedBy()).isEqualTo(SIGNER);
        assertThat(f.getSignedAt()).isEqualTo(FIXED.instant());
    }

    @Test
    void mark_signed_is_idempotent() {
        LabFile f = newOpenLabFile();
        var laterClock = Clock.fixed(Instant.parse("2026-05-28T15:00:00Z"), ZoneOffset.UTC);

        f.markSigned(SIGNER, FIXED);
        f.markSigned("01OTHERAAAAAAAAAAAAAAAAAAA", laterClock);

        assertThat(f.getStatus()).isEqualTo(LabFileStatus.SIGNED);
        assertThat(f.getSignedBy()).isEqualTo(SIGNER);
        assertThat(f.getSignedAt()).isEqualTo(FIXED.instant());
    }

    private LabFile newOpenLabFile() {
        var cmd =
                new NewLabFile(
                        "01PATIENT",
                        "01PRESCRIBER",
                        "01PRELEVEUR",
                        false,
                        List.of("GLUC"),
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        null,
                        null,
                        null);
        return LabFile.open(cmd, REG, "01CREATOR", FIXED);
    }
}
