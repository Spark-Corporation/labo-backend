package org.sparkcorporation.lims.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class StatsByDisciplineIT extends StatsIntegrationTestBase {

    @LocalServerPort int port;

    @Test
    void counts_result_lines_and_abnormals_per_component_discipline() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(2, ChronoUnit.HOURS);

        String discBio = seedDiscipline("BIO", "Biochimie");
        String discHema = seedDiscipline("HEMA", "Hematologie");
        String aGlu = seedAnalysis("GLU", "Glucose", discBio);
        String aHb = seedAnalysis("HB", "Hemoglobine", discHema);

        String patient = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);
        String labFile = seedLabFile(patient, inRange, false, "OPEN", null, null, null);
        String result =
                seedResult(labFile, aGlu, "GLU", "BIOLOGICALLY_VALIDATED", "HOMME", "M", 12000);

        // Biochimie: two lines, one out-of-range.
        seedResultLine(result, aGlu, "GLU", 5.0, null, false, 0);
        seedResultLine(result, aGlu, "GLU", 25.0, null, true, 1);
        // Hematologie: one line, in-range.
        seedResultLine(result, aHb, "HB", 13.0, null, false, 2);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/by-discipline?from={f}&to={t}",
                                today.toString(),
                                today.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        assertThat(rows).hasSize(2);

        Map<String, Object> bio =
                rows.stream()
                        .filter(r -> "Biochimie".equals(r.get("disciplineName")))
                        .findFirst()
                        .orElseThrow();
        assertThat(((Number) bio.get("resultCount")).longValue()).isEqualTo(2);
        assertThat(((Number) bio.get("abnormalCount")).longValue()).isEqualTo(1);

        Map<String, Object> hema =
                rows.stream()
                        .filter(r -> "Hematologie".equals(r.get("disciplineName")))
                        .findFirst()
                        .orElseThrow();
        assertThat(((Number) hema.get("resultCount")).longValue()).isEqualTo(1);
        assertThat(((Number) hema.get("abnormalCount")).longValue()).isEqualTo(0);
    }

    @Test
    void empty_range_yields_no_rows() {
        // A date range with no seeded dossiers must produce an empty aggregate, not an error.
        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/by-discipline?from={f}&to={t}",
                                "2000-01-01",
                                "2000-01-31")
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        assertThat(rows).isEmpty();
    }

    @Test
    void malformed_ulid_filter_is_400() {
        // A non-ULID filter param fails @Pattern validation; the global ProblemDetailAdvice maps
        // the resulting ConstraintViolationException to 400 (not 500).
        var http = restClientFor(port, "ADMIN");
        HttpStatus status =
                http.get()
                        .uri(
                                "/api/v1/stats/by-discipline?from={f}&to={t}&insuranceId={i}",
                                "2026-05-01",
                                "2026-05-10",
                                "not-a-ulid")
                        .exchange((req, res) -> HttpStatus.valueOf(res.getStatusCode().value()));
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
