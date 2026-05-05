package org.sparkcorporation.lims.patient.infra;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.patient.domain.LabCode;
import org.sparkcorporation.lims.patient.domain.LabCodeCounter;
import org.sparkcorporation.lims.patient.domain.LabCodeGenerator;
import org.sparkcorporation.lims.patient.domain.LabCodePrefix;

/**
 * Composes a {@link LabCodePrefix}, a date in {@code yyMMdd}, and an atomic sequence into a {@link
 * LabCode}.
 */
@Component
@RequiredArgsConstructor
public class DefaultLabCodeGenerator implements LabCodeGenerator {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final LabCodeCounter counter;
    private final LabCodePrefix prefix;

    @Override
    public LabCode next(LocalDate day) {
        int seq = counter.incrementAndGet(prefix, day);
        String formatted = "%s-%s-%03d".formatted(prefix.value(), day.format(DAY_FORMAT), seq);
        return new LabCode(formatted);
    }
}
