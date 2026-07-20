package org.sparkcorporation.lims.attachment.infra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import org.sparkcorporation.lims.attachment.domain.BlobStore;
import org.sparkcorporation.lims.attachment.domain.StorageRef;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Default {@link BlobStore}: bytes under {@code lims.attachment.storage.fs.root}, sharded {@code
 * ab/cd/<ulid>} so no directory accumulates millions of entries (spec §6). The content type is not
 * used by this backend (the metadata row owns it) but stays on the SPI for S3.
 */
@Component
@ConditionalOnProperty(
        name = "lims.attachment.storage.backend",
        havingValue = "filesystem",
        matchIfMissing = true)
public class FilesystemBlobStore implements BlobStore {

    private static final int SHARD_CHARS = 2;

    private final Path root;

    public FilesystemBlobStore(AttachmentStorageProperties props) {
        this.root = Path.of(props.getFs().getRoot()).toAbsolutePath().normalize();
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
        var target = root.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write blob " + key, e);
        }
        return new StorageRef(StorageRef.Backend.FILESYSTEM, key);
    }

    @Override
    public byte[] get(StorageRef ref) {
        try {
            return Files.readAllBytes(root.resolve(ref.key()));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read blob " + ref.key(), e);
        }
    }

    @Override
    public String uriOf(StorageRef ref) {
        return root.resolve(ref.key()).toUri().toString();
    }
}
