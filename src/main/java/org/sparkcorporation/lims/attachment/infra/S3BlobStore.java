package org.sparkcorporation.lims.attachment.infra;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import org.sparkcorporation.lims.attachment.domain.BlobStore;
import org.sparkcorporation.lims.attachment.domain.StorageRef;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * S3-compatible {@link BlobStore} (spec §6). Registered by {@link S3BlobStoreConfig} when {@code
 * lims.attachment.storage.backend=s3} — deliberately NOT {@code @Component}, so the filesystem
 * default never races it.
 */
public class S3BlobStore implements BlobStore {

    private static final int SHARD_CHARS = 2;

    private final S3Client client;
    private final String bucket;

    public S3BlobStore(S3Client client, AttachmentStorageProperties props) {
        this.client = client;
        this.bucket = props.getS3().getBucket();
    }

    @Override
    public StorageRef put(byte[] content, String contentType) {
        var ulid = Ulid.generate();
        var key =
                ulid.substring(0, SHARD_CHARS)
                        + "/"
                        + ulid.substring(SHARD_CHARS, SHARD_CHARS * 2)
                        + "/"
                        + ulid;
        client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
        return new StorageRef(StorageRef.Backend.S3, key);
    }

    @Override
    public byte[] get(StorageRef ref) {
        return client.getObjectAsBytes(
                        GetObjectRequest.builder().bucket(bucket).key(ref.key()).build())
                .asByteArray();
    }

    @Override
    public String uriOf(StorageRef ref) {
        return "s3://" + bucket + "/" + ref.key();
    }
}
