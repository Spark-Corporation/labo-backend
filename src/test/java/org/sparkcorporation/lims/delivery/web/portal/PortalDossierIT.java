package org.sparkcorporation.lims.delivery.web.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.order.api.LabFileId;
import org.sparkcorporation.lims.patient.web.CreatePatientRequest;
import org.sparkcorporation.lims.result.api.ResultApi;
import org.sparkcorporation.lims.result.api.ResultCreated;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for the patient-portal dossier endpoints: {@code GET /api/v1/portal/dossiers} (list
 * own PUBLISHED dossiers) and {@code GET /api/v1/portal/dossiers/{id}/pdf} (download own published
 * compte-rendu). Cross-patient and unpublished access is 404 (indistinguishable from absent);
 * missing/staff tokens are 401 (the {@code PortalAuthFilter} gate).
 *
 * <p>Patients are registered via {@code POST /api/v1/patients} (with portal credentials) so they
 * can log into the portal; discipline + analysis fixtures are seeded via raw SQL (the delivery
 * module cannot import other modules' entities — Modulith boundary). The dossier-driving flow (open
 * → enter → validate-technical → validate-biological → publish) mirrors {@code
 * ResultEntryAndValidationIT} / {@code PublishIT}.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PortalDossierIT.Events.class)
class PortalDossierIT extends IntegrationTestBase {

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ResultApi resultApi;
    @Autowired private Events events;

    private RestClient admin;
    private RestClient technician;
    private RestClient biologist;

    private String analysisCode;

    @BeforeEach
    void setUp() {
        admin = restClientFor(port, "ADMIN");
        technician = restClientFor(port, "TECHNICIAN");
        biologist = restClientFor(port, "BIOLOGIST");
        events.clear();
        var disciplineId = seedDiscipline();
        analysisCode = "GLU_" + Ulid.generate().substring(10, 16);
        seedAnalysis(analysisCode, disciplineId);
    }

    @Test
    void lists_only_own_published_dossiers() {
        String tokenA = portalLogin("alice-d", "passA-1234"); // patient A owns 1 PUBLISHED dossier
        var clientA = portalClient(port, tokenA);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dossiers =
                clientA.get().uri("/api/v1/portal/dossiers").retrieve().body(List.class);

        assertThat(dossiers).hasSize(1);
    }

    @Test
    void cannot_download_another_patients_pdf_returns_404() {
        String tokenA = portalLogin("alice2-d", "passA-1234");
        String othersPublishedLabFileId = aPublishedDossierOwnedBy("bob-d", "passB-1234");
        var clientA = portalClient(port, tokenA);

        assertThatThrownBy(
                        () ->
                                clientA.get()
                                        .uri(
                                                "/api/v1/portal/dossiers/{id}/pdf",
                                                othersPublishedLabFileId)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(404)));
    }

    @Test
    void own_unpublished_dossier_pdf_returns_404() {
        String tokenA = portalLogin("carol-d", "passC-1234");
        String ownUnpublished = anOpenDossierOwnedBy(patientIdFor("carol-d"));
        var clientA = portalClient(port, tokenA);

        assertThatThrownBy(
                        () ->
                                clientA.get()
                                        .uri("/api/v1/portal/dossiers/{id}/pdf", ownUnpublished)
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(404)));
    }

    @Test
    void own_published_dossier_pdf_returns_200_pdf_bytes() {
        String tokenA = portalLogin("dave-d", "passD-1234");
        String ownPublished = lastPublishedFor("dave-d");
        var clientA = portalClient(port, tokenA);

        var resp =
                clientA.get()
                        .uri("/api/v1/portal/dossiers/{id}/pdf", ownPublished)
                        .retrieve()
                        .toEntity(byte[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(resp.getBody()).isNotEmpty();
    }

    @Test
    void missing_token_returns_401() {
        var anon = restClientAnonymous(port);
        assertThatThrownBy(
                        () ->
                                anon.get()
                                        .uri("/api/v1/portal/dossiers")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    @Test
    void staff_jwt_is_rejected_on_portal_endpoints_401() {
        String staffJwt = loginAs(port, "biologist@test.local", "test-password");
        var staffOnPortal = portalClient(port, staffJwt);
        assertThatThrownBy(
                        () ->
                                staffOnPortal
                                        .get()
                                        .uri("/api/v1/portal/dossiers")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    @Test
    void portal_jwt_is_rejected_on_staff_endpoint() {
        String portalToken = portalLogin("erin-d", "passE-1234");
        var portalOnStaff = portalClient(port, portalToken);
        assertThatThrownBy(
                        () ->
                                portalOnStaff
                                        .get()
                                        .uri("/api/v1/lab-files")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    @Test
    void malformed_token_returns_401() {
        var malformed = portalClient(port, "garbage.jwt.value");
        assertThatThrownBy(
                        () ->
                                malformed
                                        .get()
                                        .uri("/api/v1/portal/dossiers")
                                        .retrieve()
                                        .toBodilessEntity())
                .isInstanceOfSatisfying(
                        HttpClientErrorException.class,
                        ex ->
                                assertThat(ex.getStatusCode())
                                        .isEqualTo(HttpStatusCode.valueOf(401)));
    }

    // ----------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------

    /**
     * Registers a patient with the given portal credentials, publishes one dossier for them, then
     * logs into the portal and returns the issued portal token.
     */
    private String portalLogin(String login, String password) {
        String patientId = registerPatientWithPortal(login, password);
        publishDossierFor(patientId);
        return loginToPortal(login, password);
    }

    /**
     * Registers a patient, publishes one dossier for them, and returns the dossier's lab-file id.
     */
    private String aPublishedDossierOwnedBy(String login, String password) {
        String patientId = registerPatientWithPortal(login, password);
        return publishDossierFor(patientId);
    }

    /**
     * Opens a dossier (no validation, never published) for the given patient and returns its id.
     */
    private String anOpenDossierOwnedBy(String patientId) {
        return openLabFile(patientId);
    }

    private String registerPatientWithPortal(String login, String password) {
        CreatePatientRequest req =
                new CreatePatientRequest(
                        new CreatePatientRequest.NameRequest("DOE", "Jane", null),
                        Civility.MADAME,
                        Sex.F,
                        null,
                        null,
                        null,
                        null,
                        null,
                        new CreatePatientRequest.ContactRequest(null, null),
                        null,
                        null,
                        null,
                        login,
                        password);
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> created =
                admin.post()
                        .uri("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(req)
                        .retrieve()
                        .toEntity(Map.class);
        String patientId = (String) created.getBody().get("id");
        patientIds.put(login, patientId);
        return patientId;
    }

    private String loginToPortal(String login, String password) {
        var anon = restClientAnonymous(port);
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                anon.post()
                        .uri("/api/v1/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("portalLogin", login, "password", password))
                        .retrieve()
                        .body(Map.class);
        return (String) body.get("token");
    }

    /**
     * Drives open → enter → validate-technical → validate-biological → sign → publish; returns
     * labFileId. Publication now requires the dossier to be {@code SIGNED} first (SP3), so the
     * BIOLOGIST electronically signs it before publishing.
     */
    private String publishDossierFor(String patientId) {
        String labFileId = openLabFile(patientId);
        String resultId = resultIdFor(labFileId);
        var lines = resultApi.findActiveByLabFile(new LabFileId(labFileId));
        var lineId = lines.get(0).lines().get(0).id().value();

        technician
                .patch()
                .uri("/api/v1/results/" + resultId + "/lines/" + lineId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("value", Map.of("kind", "REAL", "value", "1.234")))
                .retrieve()
                .toBodilessEntity();
        technician
                .post()
                .uri("/api/v1/results/" + resultId + ":validate-technical")
                .retrieve()
                .toBodilessEntity();
        biologist
                .post()
                .uri("/api/v1/results/" + resultId + ":validate-biological")
                .retrieve()
                .toBodilessEntity();
        biologist
                .post()
                .uri("/api/v1/lab-files/{id}:sign", labFileId)
                .retrieve()
                .toBodilessEntity();
        biologist
                .post()
                .uri("/api/v1/lab-files/{id}:publish", labFileId)
                .retrieve()
                .toBodilessEntity();
        publishedByLogin.put(loginFor(patientId), labFileId);
        return labFileId;
    }

    private String openLabFile(String patientId) {
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> created =
                admin.post()
                        .uri("/api/v1/lab-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "patientId",
                                        patientId,
                                        "urgent",
                                        false,
                                        "analyses",
                                        List.of(analysisCode)))
                        .retrieve()
                        .toEntity(Map.class);
        String labFileId = (String) created.getBody().get("id");
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(
                        () ->
                                assertThat(events.resultCreated)
                                        .anyMatch(e -> e.labFileId().value().equals(labFileId)));
        return labFileId;
    }

    private String resultIdFor(String labFileId) {
        return events.resultCreated.stream()
                .filter(e -> e.labFileId().value().equals(labFileId))
                .findFirst()
                .orElseThrow()
                .resultId()
                .value();
    }

    private final Map<String, String> patientIds = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, String> publishedByLogin =
            new java.util.concurrent.ConcurrentHashMap<>();

    private String patientIdFor(String login) {
        return patientIds.get(login);
    }

    private String loginFor(String patientId) {
        return patientIds.entrySet().stream()
                .filter(e -> e.getValue().equals(patientId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    private String lastPublishedFor(String login) {
        return publishedByLogin.get(login);
    }

    /** Records {@link ResultCreated} so the dossier-driving flow can resolve result ids. */
    @TestConfiguration
    static class Events {
        final List<ResultCreated> resultCreated = new CopyOnWriteArrayList<>();

        @EventListener
        void onCreated(ResultCreated e) {
            resultCreated.add(e);
        }

        void clear() {
            resultCreated.clear();
        }
    }

    private String seedDiscipline() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, 'Biochimie', 'BIOCHIMIE', 0)",
                id,
                "BIOCHEM-" + id.substring(10, 16));
        return id;
    }

    private void seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, 'Glucose', 'SIMPLE',"
                        + " 10.00, 1, ?, 'REAL', 0)",
                id,
                code,
                disciplineId);
    }
}
