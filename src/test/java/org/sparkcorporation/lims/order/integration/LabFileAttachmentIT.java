package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LabFileAttachmentIT extends IntegrationTestBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final byte[] PDF = "%PDF-1.7 acceptance".getBytes(StandardCharsets.US_ASCII);
    private static final int OVERSIZE = 25 * 1024 * 1024 + 1;

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void uploadListDownloadSoftDeleteRoundTrip() {
        var receptionist = restClientFor(port, "RECEPTIONIST");
        var labFileId = seedLabFile();

        // upload
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                receptionist
                        .post()
                        .uri("/api/v1/lab-files/{id}/attachments", labFileId)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(multipart("scan.pdf", PDF))
                        .retrieve()
                        .body(Map.class);
        assertThat(created.get("filename")).isEqualTo("scan.pdf");
        assertThat(created.get("contentType")).isEqualTo("application/pdf");
        var attachmentId = idOf(created);

        // row landed with detected type + hash
        var row =
                jdbc.queryForMap(
                        "SELECT attachable_type, content_type, deleted_at FROM attachments"
                                + " WHERE id = ?",
                        attachmentId);
        assertThat(row.get("attachable_type")).isEqualTo("LAB_FILE");
        assertThat(row.get("content_type")).isEqualTo("application/pdf");
        assertThat(row.get("deleted_at")).isNull();

        // list
        List<?> listed =
                receptionist
                        .get()
                        .uri("/api/v1/lab-files/{id}/attachments", labFileId)
                        .retrieve()
                        .body(List.class);
        assertThat(listed).hasSize(1);

        // download streams the original bytes with the detected type
        var download =
                receptionist
                        .get()
                        .uri("/api/v1/lab-files/{id}/attachments/{att}", labFileId, attachmentId)
                        .retrieve()
                        .toEntity(byte[].class);
        assertThat(download.getBody()).containsExactly(PDF);
        assertThat(download.getHeaders().getContentType()).hasToString("application/pdf");

        // soft delete
        receptionist
                .delete()
                .uri("/api/v1/lab-files/{id}/attachments/{att}", labFileId, attachmentId)
                .retrieve()
                .toBodilessEntity();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT deleted_at IS NOT NULL FROM attachments WHERE id = ?",
                                Boolean.class,
                                attachmentId))
                .isTrue();

        // gone from list, download 404s
        List<?> afterDelete =
                receptionist
                        .get()
                        .uri("/api/v1/lab-files/{id}/attachments", labFileId)
                        .retrieve()
                        .body(List.class);
        assertThat(afterDelete).isEmpty();
        assertThatThrownBy(
                        () ->
                                receptionist
                                        .get()
                                        .uri(
                                                "/api/v1/lab-files/{id}/attachments/{att}",
                                                labFileId,
                                                attachmentId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void uploadWithoutLabFileCreateReturns403() {
        var cashier = restClientFor(port, "CASHIER");
        var labFileId = seedLabFile();

        assertThatThrownBy(
                        () ->
                                cashier.post()
                                        .uri("/api/v1/lab-files/{id}/attachments", labFileId)
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(multipart("scan.pdf", PDF))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(403)));
    }

    @Test
    void uploadToUnknownLabFileReturns404() {
        var receptionist = restClientFor(port, "RECEPTIONIST");

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}/attachments", Ulid.generate())
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(multipart("scan.pdf", PDF))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void oversizeUploadReturns413EvenForUnknownTarget() {
        // spec §4 ordering note: the multipart limit fires before the controller body
        var receptionist = restClientFor(port, "RECEPTIONIST");

        assertThatThrownBy(
                        () ->
                                receptionist
                                        .post()
                                        .uri("/api/v1/lab-files/{id}/attachments", Ulid.generate())
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(multipart("big.bin", new byte[OVERSIZE]))
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(413));
    }

    // ----- helpers --------------------------------------------------------------------------

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

    private static String idOf(Map<String, Object> created) {
        // AttachmentSummary.id is a plain ULID string (flat JSON shape).
        return (String) created.get("id");
    }

    /** Mirrors LabFileApiBatchIT.seedLabFile — LF-yyMMdd-NNNN; REG_SEQ inherited from base. */
    private String seedLabFile() {
        var patientId = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                patientId,
                "LAB-" + patientId.substring(0, 26));
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
}
