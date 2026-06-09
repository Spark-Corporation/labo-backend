package org.sparkcorporation.lims.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;

class LabFileMarkPrintedTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-06-05T10:00:00Z"), ZoneOffset.UTC);
    private static final LabFileRegistrationNumber REG =
            new LabFileRegistrationNumber("LF-260605-0001");

    @Test
    void mark_printed_sets_printed_at() {
        LabFile f = newOpenLabFile();
        var stamp = Instant.parse("2026-06-05T11:00:00Z");

        f.markPrinted(stamp);

        assertThat(f.getPrintedAt()).isEqualTo(stamp);
    }

    @Test
    void mark_printed_overwrites_on_reprint() {
        LabFile f = newOpenLabFile();
        var first = Instant.parse("2026-06-05T11:00:00Z");
        var second = Instant.parse("2026-06-05T12:00:00Z");

        f.markPrinted(first);
        f.markPrinted(second);

        assertThat(f.getPrintedAt()).isEqualTo(second);
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
