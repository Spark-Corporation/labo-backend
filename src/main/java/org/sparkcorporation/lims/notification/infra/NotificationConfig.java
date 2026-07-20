package org.sparkcorporation.lims.notification.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Notification-module Spring configuration. Registers {@link NotificationProperties} so the poller
 * and adapters bind their cadence / retry policy / channel toggles at startup.
 */
@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {}
