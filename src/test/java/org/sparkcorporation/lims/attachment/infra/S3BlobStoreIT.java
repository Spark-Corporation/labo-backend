package org.sparkcorporation.lims.attachment.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import org.sparkcorporation.lims.attachment.domain.StorageRef;

/**
 * Round-trips the S3 backend against a real S3-compatible store (MinIO). Per-class container is
 * fine here: this is the only MinIO IT and it carries no Spring context to cache.
 */
@Tag("integration")
@Testcontainers
class S3BlobStoreIT {

    @Container
    private static final MinIOContainer MINIO =
            new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z");

    private static final String BUCKET = "lims-attachments-it";

    @BeforeAll
    static void createBucket() {
        var props = props();
        var client = new S3BlobStoreConfig().s3Client(props);
        client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        client.close();
    }

    private static AttachmentStorageProperties props() {
        var props = new AttachmentStorageProperties();
        props.setBackend("s3");
        props.getS3().setBucket(BUCKET);
        props.getS3().setEndpoint(MINIO.getS3URL());
        props.getS3().setRegion("us-east-1");
        props.getS3().setAccessKey(MINIO.getUserName());
        props.getS3().setSecretKey(MINIO.getPassword());
        props.getS3().setPathStyle(true);
        return props;
    }

    private static S3BlobStore store() {
        var props = props();
        var client = new S3BlobStoreConfig().s3Client(props);
        return new S3BlobStore(client, props);
    }

    @Test
    void putThenGetRoundTripsAndUriIsS3Scheme() {
        var store = store();
        byte[] content = {'%', 'P', 'D', 'F', '-'};

        StorageRef ref = store.put(content, "application/pdf");

        assertThat(ref.backend()).isEqualTo(StorageRef.Backend.S3);
        assertThat(store.get(ref)).containsExactly(content);
        assertThat(store.uriOf(ref)).isEqualTo("s3://" + BUCKET + "/" + ref.key());
    }

    @Test
    void getOnAMissingKeyThrows() {
        var store = store();
        var ghost = new StorageRef(StorageRef.Backend.S3, "ZZ/ZZ/01HZX0000000000000000G0NE1");
        assertThatThrownBy(() -> store.get(ghost)).isInstanceOf(NoSuchKeyException.class);
    }
}
