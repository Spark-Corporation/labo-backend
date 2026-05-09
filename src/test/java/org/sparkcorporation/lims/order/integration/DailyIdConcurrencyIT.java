package org.sparkcorporation.lims.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Concurrency acceptance IT for the daily-ID counter per spec §9.3 — load-bearing test for the
 * UPSERT-with-RETURNING serialization claim. Spawns 5 disciplines × 20 parallel sample-creation
 * calls each (100 total) across a shared thread pool of 20, then asserts:
 *
 * <ol>
 *   <li>No duplicate daily-IDs across all 100 responses.
 *   <li>Each per-prefix sequence is exactly {@code 001..020} with no gaps.
 * </ol>
 *
 * <p>If the UPSERT serialization were broken, two threads racing on the same {@code (day,
 * disciplinePrefix)} bucket would either return the same sequence value (duplicate detection fails)
 * or produce gaps (gap detection fails). The test exercises both edge conditions.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DailyIdConcurrencyIT extends IntegrationTestBase {

    private static final int DISCIPLINES = 5;
    private static final int SAMPLES_PER_DISCIPLINE = 20;
    private static final int THREAD_POOL_SIZE = 20;

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbc;

    private RestClient http;

    private String patientId;

    /** prefix index 1..5 → {@code (analysisId, analysisCode)} for that discipline. */
    private final Map<Integer, AnalysisFixture> fixtureByPrefix = new java.util.HashMap<>();

    private String recipientId;

    @BeforeEach
    void setUp() {
        http = restClientFor(port, "ADMIN");
        // Reset the per-(day, prefix) sample-daily-id counter so the load-bearing
        // {@code 001..020 sequential, no gaps} invariant is asserted from a known zero-state. The
        // Testcontainers Postgres is JVM-scoped (shared across ITs in the same Surefire fork) so
        // any earlier sample-creation IT in the same fork would have already bumped the counter.
        // Order matters: samples have a UNIQUE(daily_id) constraint so they must go before the
        // counter reset, otherwise the resequenced (001..020) inserts would collide with leftover
        // rows from prior tests.
        jdbc.update("DELETE FROM samples");
        jdbc.update("DELETE FROM sample_daily_id_counters");
        patientId = seedPatient();
        recipientId = seedRecipient();
        for (int prefix = 1; prefix <= DISCIPLINES; prefix++) {
            var disciplineId = seedDiscipline(enumNameFor(prefix));
            var code = enumNameFor(prefix) + "_" + Ulid.generate().substring(10, 16);
            var analysisId = seedAnalysis(code, disciplineId);
            fixtureByPrefix.put(prefix, new AnalysisFixture(analysisId, code));
        }
    }

    @Test
    void parallel_sample_creation_across_5_disciplines_produces_no_duplicate_daily_ids()
            throws Exception {
        // For each (prefix, i) pair, open a dedicated lab file with the analysis for that
        // discipline so the in-lab POST has a valid file context. Lab-file creation is sequential
        // here (cheap; the load-bearing concurrency is on the daily-ID counter, exercised via the
        // sample POSTs below).
        var labFileIdsByPrefix = new java.util.HashMap<Integer, List<String>>();
        for (int prefix = 1; prefix <= DISCIPLINES; prefix++) {
            var fix = fixtureByPrefix.get(prefix);
            var ids = new ArrayList<String>();
            for (int i = 0; i < SAMPLES_PER_DISCIPLINE; i++) {
                ids.add(openLabFile(fix.code()));
            }
            labFileIdsByPrefix.put(prefix, ids);
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        var futures = new ArrayList<Future<String>>();
        for (int prefix = 1; prefix <= DISCIPLINES; prefix++) {
            var fix = fixtureByPrefix.get(prefix);
            var ids = labFileIdsByPrefix.get(prefix);
            for (int i = 0; i < SAMPLES_PER_DISCIPLINE; i++) {
                final int idx = i;
                final String labFileId = ids.get(idx);
                futures.add(executor.submit(() -> postInLabSample(labFileId, fix.analysisId())));
            }
        }
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            throw new AssertionError("executor did not terminate in 60s");
        }

        var ids = new ArrayList<String>();
        for (var f : futures) {
            ids.add(f.get());
        }

        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids).hasSize(DISCIPLINES * SAMPLES_PER_DISCIPLINE);

        for (int prefix = 1; prefix <= DISCIPLINES; prefix++) {
            String prefixStr = String.valueOf(prefix);
            var byPrefix = ids.stream().filter(s -> s.startsWith(prefixStr)).sorted().toList();
            assertThat(byPrefix)
                    .as(
                            "prefix %d must have exactly %d unique sequential ids",
                            prefix, SAMPLES_PER_DISCIPLINE)
                    .hasSize(SAMPLES_PER_DISCIPLINE);
            for (int j = 0; j < SAMPLES_PER_DISCIPLINE; j++) {
                assertThat(byPrefix.get(j)).endsWith("-%03d".formatted(j + 1));
            }
        }
    }

    private String openLabFile(String analysisCode) {
        @SuppressWarnings("unchecked")
        Map<String, Object> created =
                http.post()
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
                        .body(Map.class);
        return (String) created.get("id");
    }

    private String postInLabSample(String labFileId, String analysisId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.post()
                        .uri("/api/v1/lab-files/" + labFileId + "/samples:in-lab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Map.of(
                                        "analysisId",
                                        analysisId,
                                        "recipientId",
                                        recipientId,
                                        "collectedAt",
                                        Instant.now().toString()))
                        .retrieve()
                        .body(Map.class);
        return (String) body.get("dailyId");
    }

    private static String enumNameFor(int prefix) {
        return switch (prefix) {
            case 1 -> "BIOCHIMIE";
            case 2 -> "HEMATOLOGIE";
            case 3 -> "IMMUNO_SEROLOGIE";
            case 4 -> "MICROBIOLOGIE";
            case 5 -> "BIOLOGIE_MOLECULAIRE";
            default -> throw new IllegalArgumentException("unknown prefix: " + prefix);
        };
    }

    private String seedPatient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO patients (id, lab_code, last_name, civility, sex, insured, version)"
                        + " VALUES (?, ?, 'DOE', 'MADAME', 'F', false, 0)",
                id,
                "LAB-" + id.substring(0, 10));
        return id;
    }

    private String seedDiscipline(String enumName) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO disciplines (id, code, name, discipline_prefix, version) VALUES (?,"
                        + " ?, ?, ?, 0)",
                id,
                enumName + "-" + id.substring(10, 16),
                enumName,
                enumName);
        return id;
    }

    private String seedAnalysis(String code, String disciplineId) {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO analyses (id, code, name, kind, indiciary_value_b, label_count,"
                        + " discipline_id, result_type, version) VALUES (?, ?, ?, 'SIMPLE', 10.00,"
                        + " 1, ?, 'REAL', 0)",
                id,
                code,
                code,
                disciplineId);
        return id;
    }

    private String seedRecipient() {
        var id = Ulid.generate();
        jdbc.update(
                "INSERT INTO recipients (id, code, name, version) VALUES (?, ?, 'Tube EDTA', 0)",
                id,
                "TUBE-" + id.substring(10, 16));
        return id;
    }

    /** Fixture pair for a single discipline. */
    private record AnalysisFixture(String analysisId, String code) {}
}
