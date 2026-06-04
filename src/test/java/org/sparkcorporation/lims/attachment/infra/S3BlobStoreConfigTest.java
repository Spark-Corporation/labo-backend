package org.sparkcorporation.lims.attachment.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class S3BlobStoreConfigTest {

    @Test
    void blankEndpointBuildsARealAwsClient() {
        var props = new AttachmentStorageProperties();
        props.setBackend("s3");
        props.getS3().setEndpoint("");
        props.getS3().setRegion("us-east-1");

        try (var client = new S3BlobStoreConfig().s3Client(props)) {
            assertThat(client).isNotNull();
        }
    }
}
