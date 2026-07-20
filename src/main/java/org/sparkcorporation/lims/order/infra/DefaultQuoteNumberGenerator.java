package org.sparkcorporation.lims.order.infra;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.order.api.QuoteNumber;
import org.sparkcorporation.lims.order.domain.QuoteNumberCounter;

/**
 * Composes {@code DEV-yyMMdd-NNNN} quote numbers (spec §4.6). Pure function over the injected
 * {@link QuoteNumberCounter} — persistence / concurrency is the counter's concern. Mirrors {@link
 * DefaultLabFileRegistrationGenerator}.
 */
@Component
@RequiredArgsConstructor
public class DefaultQuoteNumberGenerator {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final QuoteNumberCounter counter;

    public QuoteNumber next(LocalDate day) {
        int seq = counter.incrementAndGet(day);
        return new QuoteNumber("DEV-%s-%04d".formatted(day.format(DAY_FORMAT), seq));
    }
}
