package org.sparkcorporation.lims.attachment.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.sparkcorporation.lims.attachment.domain.StorageRef;

class FilesystemBlobStoreTest {

    @TempDir Path root;

    private FilesystemBlobStore store() {
        var props = new AttachmentStorageProperties();
        props.getFs().setRoot(root.toString());
        return new FilesystemBlobStore(props);
    }

    @Test
    void putThenGetRoundTrips() {
        var store = store();
        byte[] content = {'%', 'P', 'D', 'F', '-'};

        StorageRef ref = store.put(content, "application/pdf");

        assertThat(ref.backend()).isEqualTo(StorageRef.Backend.FILESYSTEM);
        assertThat(store.get(ref)).containsExactly(content);
    }

    @Test
    void keysAreShardedTwoLevelsDeep() {
        var ref = store().put(new byte[] {1}, "application/octet-stream");
        // ab/cd/<26-char-ulid> — guards against one flat dir with millions of files
        assertThat(ref.key()).matches("[0-9A-Z]{2}/[0-9A-Z]{2}/[0-9A-Z]{26}");
    }

    @Test
    void uriOfIsAFileUriUnderTheRoot() {
        var store = store();
        var ref = store.put(new byte[] {1}, "application/octet-stream");
        assertThat(store.uriOf(ref)).startsWith("file://").contains(ref.key());
    }

    @Test
    void getOnAMissingKeyThrowsUncheckedIo() {
        var store = store();
        var ghost =
                new StorageRef(StorageRef.Backend.FILESYSTEM, "ZZ/ZZ/01HZX0000000000000000G0NE1");
        assertThatThrownBy(() -> store.get(ghost)).isInstanceOf(UncheckedIOException.class);
    }
}
