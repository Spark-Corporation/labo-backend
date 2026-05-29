package org.sparkcorporation.lims.notification.infra;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code lims.notification.*} — the poller's cadence and retry policy plus the per-channel
 * stub toggles. Defaults mirror spec §4.2: 30 s poll, 5 attempts, 1→16 min exponential backoff,
 * batch of 50; both stub adapters enabled (dev/test), toggled off in production once real adapters
 * land.
 */
@ConfigurationProperties("lims.notification")
public class NotificationProperties {

    private static final int DEFAULT_POLL_INTERVAL_SECONDS = 30;
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    private static final int DEFAULT_BATCH_SIZE = 50;

    private Duration pollInterval = Duration.ofSeconds(DEFAULT_POLL_INTERVAL_SECONDS);
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private Duration backoffBase = Duration.ofMinutes(1);
    private Channel email = new Channel();
    private Channel whatsapp = new Channel();

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getBackoffBase() {
        return backoffBase;
    }

    public void setBackoffBase(Duration backoffBase) {
        this.backoffBase = backoffBase;
    }

    public Channel getEmail() {
        return email;
    }

    public void setEmail(Channel email) {
        this.email = email;
    }

    public Channel getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(Channel whatsapp) {
        this.whatsapp = whatsapp;
    }

    /** Per-channel settings — {@code stub = true} routes through the logging stub adapter. */
    public static class Channel {

        private boolean stub = true;

        public boolean isStub() {
            return stub;
        }

        public void setStub(boolean stub) {
            this.stub = stub;
        }
    }
}
