package org.sparkcorporation.lims.attachment.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link AttachmentStorageProperties} so binding failures surface at boot, mirroring
 * {@code order/web/OrderConfig}. BlobStore implementations self-select via
 * {@code @ConditionalOnProperty} on {@code lims.attachment.storage.backend}.
 */
@Configuration
@EnableConfigurationProperties(AttachmentStorageProperties.class)
public class AttachmentConfig {}
