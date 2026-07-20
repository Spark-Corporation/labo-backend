package org.sparkcorporation.lims.pdf.infra;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded executor used by {@code AsyncPdfWorker} via {@code @Async("pdfRenderExecutor")}. Also
 * enables Spring's scheduling support for {@code PdfRenderJobCleanupScheduler}'s
 * {@code @Scheduled}.
 *
 * <p>{@link ThreadPoolExecutor.AbortPolicy} surfaces queue saturation as a {@link
 * java.util.concurrent.RejectedExecutionException} which the worker maps to the {@code
 * executor_saturated} reason tag.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class PdfRenderExecutorConfig {

    @Bean("pdfRenderExecutor")
    public ThreadPoolTaskExecutor pdfRenderExecutor(
            @Value("${lims.pdf.executor.core-pool-size:2}") int corePoolSize,
            @Value("${lims.pdf.executor.max-pool-size:4}") int maxPoolSize,
            @Value("${lims.pdf.executor.queue-capacity:50}") int queueCapacity) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("pdf-render-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setKeepAliveSeconds((int) TimeUnit.MINUTES.toSeconds(1));
        executor.initialize();
        return executor;
    }
}
