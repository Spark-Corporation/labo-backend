package org.sparkcorporation.lims.order.infra;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.order.api.SampleDailyId;
import org.sparkcorporation.lims.order.domain.SampleDailyIdCounter;

/**
 * Composes {@code <disciplinePrefix><ddMMyy>-<seq>} sample daily-IDs (CDC §IV; spec §4.6). Pure
 * function over the injected {@link SampleDailyIdCounter} — concurrency / persistence is the
 * counter's concern.
 */
@Component
@RequiredArgsConstructor
public class DefaultSampleDailyIdGenerator {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("ddMMyy");
    private static final int DISCIPLINE_COUNT = 5;

    private final SampleDailyIdCounter counter;

    /**
     * Returns the next {@link SampleDailyId} for the given {@code disciplinePrefix} (1..5) on the
     * given {@code day}.
     *
     * @throws IllegalArgumentException if {@code disciplinePrefix} is not in {@code 1..5}.
     */
    public SampleDailyId next(int disciplinePrefix, LocalDate day) {
        if (disciplinePrefix < 1 || disciplinePrefix > DISCIPLINE_COUNT) {
            throw new IllegalArgumentException(
                    "disciplinePrefix must be 1.." + DISCIPLINE_COUNT + ": " + disciplinePrefix);
        }
        int seq = counter.incrementAndGet(day, disciplinePrefix);
        return new SampleDailyId(
                "%d%s-%03d".formatted(disciplinePrefix, day.format(DAY_FORMAT), seq));
    }
}
