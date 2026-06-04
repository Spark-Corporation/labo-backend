package org.sparkcorporation.lims.attachment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AttachmentUploadTest {

    private static final String TARGET = "01HZX0000000000000000TGT01";
    private static final String UPLOADER = "01HZX0000000000000000VSR01";

    private static AttachmentUpload upload(byte[] bytes) {
        return new AttachmentUpload(
                AttachableType.LAB_FILE, TARGET, "scan.pdf", "application/pdf", bytes, UPLOADER);
    }

    @Test
    void clonesBytesOnConstructionAndAccess() {
        byte[] original = {1, 2, 3};
        var cmd = upload(original);
        original[0] = 9; // mutate caller's array AFTER construction
        assertThat(cmd.bytes()).containsExactly(1, 2, 3);
        cmd.bytes()[0] = 9; // mutate accessor copy
        assertThat(cmd.bytes()).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsNonUlidTargetId() {
        assertThatThrownBy(
                        () ->
                                new AttachmentUpload(
                                        AttachableType.LAB_FILE,
                                        "nope",
                                        "scan.pdf",
                                        "application/pdf",
                                        new byte[] {1},
                                        UPLOADER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonUlidUploader() {
        assertThatThrownBy(
                        () ->
                                new AttachmentUpload(
                                        AttachableType.LAB_FILE,
                                        TARGET,
                                        "scan.pdf",
                                        "application/pdf",
                                        new byte[] {1},
                                        "nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
