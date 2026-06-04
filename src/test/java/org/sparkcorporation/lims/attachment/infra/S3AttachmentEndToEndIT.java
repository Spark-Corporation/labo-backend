package org.sparkcorporation.lims.attachment.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.containers.MinIOContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Spec §11: end-to-end upload→download with {@code backend=s3} — proves the conditional wiring
 * (S3BlobStoreConfig + S3BlobStore) and the REST path against a real S3-compatible store. The
 * dedicated property set forces a fresh Spring context (not shared with the filesystem-backend
 * ITs); that cost is deliberate and confined to this one class.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class S3AttachmentEndToEndIT extends IntegrationTestBase {

    private static final String BUCKET = "lims-attachments-e2e";
    private static final byte[] PDF = "%PDF-1.7 s3 e2e".getBytes(StandardCharsets.US_ASCII);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    private static final MinIOContainer MINIO =
            new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z");

    static {
        MINIO.start();
        try (var s3 =
                S3Client.builder()
                        .region(Region.US_EAST_1)
                        .endpointOverride(URI.create(MINIO.getS3URL()))
                        .credentialsProvider(
                                StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(
                                                MINIO.getUserName(), MINIO.getPassword())))
                        .forcePathStyle(true)
                        .build()) {
            s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("lims.attachment.storage.backend", () -> "s3");
        registry.add("lims.attachment.storage.s3.bucket", () -> BUCKET);
        registry.add("lims.attachment.storage.s3.endpoint", MINIO::getS3URL);
        registry.add("lims.attachment.storage.s3.region", () -> "us-east-1");
        registry.add("lims.attachment.storage.s3.access-key", MINIO::getUserName);
        registry.add("lims.attachment.storage.s3.secret-key", MINIO::getPassword);
        registry.add("lims.attachment.storage.s3.path-style", () -> "true");
    }

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void uploadAndDownloadRoundTripThroughS3() {
        var receptionist = restClientFor(port, "RECEPTIONIST");
        var labFileId = seedLabFile();

        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                receptionist
                        .post()
                        .uri("/api/v1/lab-files/{id}/attachments", labFileId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(multipart("scan.pdf", PDF))
                        .retrieve()
                        .body(Map.class);
        assertThat((String) created.get("storageUri")).startsWith("s3://" + BUCKET + "/");

        var row =
                jdbc.queryForMap(
                        "SELECT storage_backend, content_type FROM attachments WHERE id = ?",
                        created.get("id"));
        assertThat(row.get("storage_backend")).isEqualTo("S3");
        assertThat(row.get("content_type")).isEqualTo("application/pdf");

        var download =
                receptionist
                        .get()
                        .uri(
                                "/api/v1/lab-files/{id}/attachments/{att}",
                                labFileId,
                                created.get("id"))
                        .retrieve()
                        .toEntity(byte[].class);
        assertThat(download.getBody()).containsExactly(PDF);
    }

    // ----- helpers (mirror LabFileApiBatchIT.seedLabFile; REG_SEQ inherited from base) -------

    private String seedLabFile() {
        var patientId = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                patientId,
                "LAB-" + patientId.substring(0, 10));
        var labFileId = Ulid.generate();
        var seq = String.format("%04d", REG_SEQ.getAndIncrement());
        var regNum = "LF-" + LocalDate.now().format(DATE_FMT) + "-" + seq;
        jdbc.update(
                "INSERT INTO lab_files (id, registration_number, patient_id, urgent, status,"
                        + " created_at, updated_at, version) VALUES (?, ?, ?, false, 'OPEN',"
                        + " now(), now(), 0)",
                labFileId,
                regNum,
                patientId);
        return labFileId;
    }

    private static LinkedMultiValueMap<String, Object> multipart(String filename, byte[] bytes) {
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add(
                "file",
                new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                });
        return parts;
    }
}
