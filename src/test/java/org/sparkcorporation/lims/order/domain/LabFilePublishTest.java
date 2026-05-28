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

class LabFilePublishTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-05-07T08:00:00Z"), ZoneOffset.UTC);
    private static final LabFileRegistrationNumber REG =
            new LabFileRegistrationNumber("LF-260507-0001");

    @Test
    void publish_transitions_open_to_published() {
        LabFile f = newOpenLabFile();
        f.publish();
        assertThat(f.getStatus()).isEqualTo(LabFileStatus.PUBLISHED);
    }

    @Test
    void publish_is_idempotent() {
        LabFile f = newOpenLabFile();
        f.publish();
        f.publish();
        assertThat(f.getStatus()).isEqualTo(LabFileStatus.PUBLISHED);
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
