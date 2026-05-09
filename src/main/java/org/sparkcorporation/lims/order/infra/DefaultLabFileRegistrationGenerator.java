package org.sparkcorporation.lims.order.infra;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.order.api.LabFileRegistrationNumber;
import org.sparkcorporation.lims.order.domain.LabFileRegistrationCounter;

/**
 * Composes {@code LF-yyMMdd-NNNN} LabFile registration numbers (spec §4.7). Pure function over the
 * injected {@link LabFileRegistrationCounter} — persistence / concurrency is the counter's concern.
 */
@Component
@RequiredArgsConstructor
public class DefaultLabFileRegistrationGenerator {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final LabFileRegistrationCounter counter;

    public LabFileRegistrationNumber next(LocalDate day) {
        int seq = counter.incrementAndGet(day);
        return new LabFileRegistrationNumber("LF-%s-%04d".formatted(day.format(DAY_FORMAT), seq));
    }
}
