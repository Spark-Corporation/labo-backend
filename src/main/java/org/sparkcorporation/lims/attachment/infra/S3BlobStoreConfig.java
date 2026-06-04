package org.sparkcorporation.lims.attachment.infra;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Builds the {@link S3Client} only when {@code lims.attachment.storage.backend=s3}. An empty
 * endpoint targets real AWS (default resolution chain); a non-empty endpoint (MinIO) switches to
 * static credentials + path-style addressing.
 */
@Configuration
@ConditionalOnProperty(name = "lims.attachment.storage.backend", havingValue = "s3")
public class S3BlobStoreConfig {

    @Bean
    public S3Client s3Client(AttachmentStorageProperties props) {
        var s3 = props.getS3();
        var builder = S3Client.builder().region(Region.of(s3.getRegion()));
        if (!s3.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.getEndpoint()))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(
                                            s3.getAccessKey(), s3.getSecretKey())))
                    .forcePathStyle(s3.isPathStyle());
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
        }
        return builder.build();
    }

    @Bean
    public S3BlobStore s3BlobStore(S3Client client, AttachmentStorageProperties props) {
        return new S3BlobStore(client, props);
    }
}
