package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;

class LabFileMarkEmailedTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-29T09:15:00Z"), ZoneOffset.UTC);
    private static final LabFileRegistrationNumber REG =
            new LabFileRegistrationNumber("LF-260529-0001");

    @Test
    void mark_emailed_sets_emailed_at() {
        LabFile f = newOpenLabFile();
        var stamp = Instant.parse("2026-05-29T10:00:00Z");

        f.markEmailed(stamp);

        assertThat(f.getEmailedAt()).isEqualTo(stamp);
    }

    @Test
    void mark_emailed_overwrites_with_latest_delivery_instant() {
        LabFile f = newOpenLabFile();
        var first = Instant.parse("2026-05-29T10:00:00Z");
        var second = Instant.parse("2026-05-29T11:00:00Z");

        f.markEmailed(first);
        f.markEmailed(second);

        assertThat(f.getEmailedAt()).isEqualTo(second);
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
                        null);
        return LabFile.open(cmd, REG, "01CREATOR", FIXED);
    }
}
