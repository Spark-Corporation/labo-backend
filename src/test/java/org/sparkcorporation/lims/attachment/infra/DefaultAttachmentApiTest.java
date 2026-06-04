package org.sparkcorporation.lims.attachment.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.sparkcorporation.lims.attachment.api.AttachableType;
import org.sparkcorporation.lims.attachment.api.AttachmentId;
import org.sparkcorporation.lims.attachment.api.AttachmentNotFoundException;
import org.sparkcorporation.lims.attachment.api.AttachmentTooLargeException;
import org.sparkcorporation.lims.attachment.api.AttachmentUpload;
import org.sparkcorporation.lims.attachment.api.EmptyAttachmentException;
import org.sparkcorporation.lims.attachment.domain.BlobStore;
import org.sparkcorporation.lims.attachment.domain.StorageRef;
import org.sparkcorporation.lims.shared.id.Ulid;

class DefaultAttachmentApiTest {

    private static final String TARGET = "01HZX0000000000000000TGT01";
    private static final String UPLOADER = "01HZX0000000000000000VSR01";
    private static final Instant NOW = Instant.parse("2026-06-04T09:00:00Z");
    // SHA-256 of the 18 ASCII bytes "%PDF-1.7 fake body" — recompute if the fixture changes:
    //   printf '%%PDF-1.7 fake body' | sha256sum     (%% renders as a single %)
    private static final String PDF_FIXTURE_SHA256 =
            "b742c6e413630e7e9a57d8063054298c8a1030f683815c52771c84a47ab963f3";

    /** Records puts in a map; deterministic single-key fake. */
    private static final class FakeBlobStore implements BlobStore {
        private final Map<String, byte[]> blobs = new HashMap<>();

        @Override
        public StorageRef put(byte[] content, String contentType) {
            var key = "AA/BB/" + Ulid.generate();
            blobs.put(key, content.clone());
            return new StorageRef(StorageRef.Backend.FILESYSTEM, key);
        }

        @Override
        public byte[] get(StorageRef ref) {
            var bytes = blobs.get(ref.key());
            if (bytes == null) {
                throw new java.io.UncheckedIOException(
                        new java.io.IOException("missing " + ref.key()));
            }
            return bytes.clone();
        }

        @Override
        public String uriOf(StorageRef ref) {
            return "file:///fake/" + ref.key();
        }
    }

    private final JpaAttachmentRepository repo = mock(JpaAttachmentRepository.class);
    private final FakeBlobStore blobStore = new FakeBlobStore();
    private final DefaultAttachmentApi api =
            new DefaultAttachmentApi(
                    repo,
                    blobStore,
                    new TikaContentTypeDetector(),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    private static AttachmentUpload pdfUpload() {
        return new AttachmentUpload(
                AttachableType.LAB_FILE,
                TARGET,
                "scan.pdf",
                "image/png", // deliberately lying — detector must win
                "%PDF-1.7 fake body".getBytes(StandardCharsets.US_ASCII),
                UPLOADER);
    }

    @Test
    void uploadStoresDetectsHashesAndPersists() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summary = api.upload(pdfUpload());

        assertThat(summary.contentType()).isEqualTo("application/pdf"); // detected, not declared
        assertThat(summary.sizeBytes()).isEqualTo(18);
        assertThat(summary.sha256()).isEqualTo(PDF_FIXTURE_SHA256);
        assertThat(summary.storageUri()).startsWith("file:///fake/");
        assertThat(summary.uploadedAt()).isEqualTo(NOW);

        var captor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(repo).save(captor.capture());
        var entity = captor.getValue();
        assertThat(entity.getContentType()).isEqualTo("application/pdf");
        assertThat(entity.getSha256()).isEqualTo(PDF_FIXTURE_SHA256);
        assertThat(blobStore.get(entity.storageRef())).hasSize(18);
    }

    @Test
    void emptyUploadIsRejected() {
        var cmd =
                new AttachmentUpload(
                        AttachableType.LAB_FILE, TARGET, "x.pdf", null, new byte[0], UPLOADER);
        assertThatThrownBy(() -> api.upload(cmd)).isInstanceOf(EmptyAttachmentException.class);
    }

    @Test
    void oversizeUploadIsRejected() {
        var cmd =
                new AttachmentUpload(
                        AttachableType.LAB_FILE,
                        TARGET,
                        "x.bin",
                        null,
                        new byte[DefaultAttachmentApi.MAX_SIZE_BYTES + 1],
                        UPLOADER);
        assertThatThrownBy(() -> api.upload(cmd)).isInstanceOf(AttachmentTooLargeException.class);
    }

    @Test
    void getReturnsBytesAndStoredContentType() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var saved = api.upload(pdfUpload());
        var captor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(repo).save(captor.capture());
        when(repo.findById(saved.id())).thenReturn(Optional.of(captor.getValue()));

        var rendered = api.get(new AttachmentId(saved.id()));

        assertThat(rendered.contentType()).isEqualTo("application/pdf");
        assertThat(rendered.filename()).isEqualTo("scan.pdf");
        assertThat(rendered.bytes()).hasSize(18);
    }

    @Test
    void getUnknownIdThrowsNotFound() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.get(new AttachmentId("01HZX0000000000000000G0NE1")))
                .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void getSoftDeletedThrowsNotFound() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        api.upload(pdfUpload());
        var captor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(repo).save(captor.capture());
        var entity = captor.getValue();
        entity.softDelete(NOW);
        when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> api.get(new AttachmentId(entity.getId())))
                .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void softDeleteMarksTheRow() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        api.upload(pdfUpload());
        var captor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(repo).save(captor.capture());
        var entity = captor.getValue();
        when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));

        api.softDelete(new AttachmentId(entity.getId()));

        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void softDeleteTwiceThrowsNotFound() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        api.upload(pdfUpload());
        var captor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(repo).save(captor.capture());
        var entity = captor.getValue();
        when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));
        api.softDelete(new AttachmentId(entity.getId()));

        assertThatThrownBy(() -> api.softDelete(new AttachmentId(entity.getId())))
                .isInstanceOf(AttachmentNotFoundException.class);
    }

    @Test
    void listMapsEntitiesToSummaries() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        api.upload(pdfUpload());
        var captor = ArgumentCaptor.forClass(AttachmentEntity.class);
        verify(repo).save(captor.capture());
        when(repo.findByAttachableTypeAndAttachableIdAndDeletedAtIsNullOrderByUploadedAtDesc(
                        AttachableType.LAB_FILE, TARGET))
                .thenReturn(java.util.List.of(captor.getValue()));

        var summaries = api.list(AttachableType.LAB_FILE, TARGET);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().filename()).isEqualTo("scan.pdf");
        assertThat(summaries.getFirst().storageUri()).startsWith("file:///fake/");
    }
}
