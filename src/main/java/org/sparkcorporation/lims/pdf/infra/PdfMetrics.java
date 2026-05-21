package org.sparkcorporation.lims.pdf.infra;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PdfMetrics {

    private final MeterRegistry registry;

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimerSync(Timer.Sample sample, String documentTypeId) {
        sample.stop(
                Timer.builder("lims.pdf.render")
                        .tags(Tags.of("documentType", documentTypeId, "mode", "sync"))
                        .register(registry));
    }

    public void stopTimerAsync(Timer.Sample sample, String documentTypeId) {
        sample.stop(
                Timer.builder("lims.pdf.render")
                        .tags(Tags.of("documentType", documentTypeId, "mode", "async"))
                        .register(registry));
    }

    public void incrementFailure(String documentTypeId, String reasonTag) {
        Counter.builder("lims.pdf.render.failures")
                .tags(Tags.of("documentType", documentTypeId, "reason", reasonTag))
                .register(registry)
                .increment();
    }
}
