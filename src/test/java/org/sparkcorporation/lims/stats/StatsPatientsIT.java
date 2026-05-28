package org.sparkcorporation.lims.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class StatsPatientsIT extends StatsIntegrationTestBase {

    @LocalServerPort int port;

    @Test
    void reports_sex_split_cohort_and_age_bands() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(2, ChronoUnit.HOURS);
        Instant before = inRange.minus(40, ChronoUnit.DAYS);

        // Two male patients + one female, each with a dossier registered in range.
        // pMaleNew is created in-range (new); the other two were created before (returning).
        String pMaleNew = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);
        String pMaleOld = seedPatient("M", LocalDate.of(2018, 1, 1), before);
        String pFemaleOld = seedPatient("F", LocalDate.of(2024, 1, 1), before);

        String lfMaleNew = seedLabFile(pMaleNew, inRange, false, "OPEN", null, null, null);
        String lfMaleOld = seedLabFile(pMaleOld, inRange, false, "OPEN", null, null, null);
        String lfFemaleOld = seedLabFile(pFemaleOld, inRange, false, "OPEN", null, null, null);

        // Results carry the category snapshot that drives byAgeBand (one per patient).
        String aGlu = seedAnalysis("GLU", "Glucose", seedDiscipline("BIO", "Biochimie"));
        seedResult(lfMaleNew, aGlu, "GLU", "BIOLOGICALLY_VALIDATED", "HOMME", "M", 12000);
        seedResult(lfMaleOld, aGlu, "GLU", "BIOLOGICALLY_VALIDATED", "ENFANT", "M", 3000);
        seedResult(
                lfFemaleOld,
                aGlu,
                "GLU",
                "BIOLOGICALLY_VALIDATED",
                "NOURRISSON_NOUVEAU_NE",
                "F",
                20);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/patients?from={f}&to={t}",
                                today.toString(),
                                today.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        assertThat(((Number) body.get("allCount")).longValue()).isEqualTo(3);
        assertThat(((Number) body.get("newCount")).longValue()).isEqualTo(1);
        assertThat(((Number) body.get("returningCount")).longValue()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        Map<String, Object> bySex = (Map<String, Object>) body.get("bySex");
        assertThat(((Number) bySex.get("M")).longValue()).isEqualTo(2);
        assertThat(((Number) bySex.get("F")).longValue()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> byAgeBand = (Map<String, Object>) body.get("byAgeBand");
        assertThat(((Number) byAgeBand.get("HOMME")).longValue()).isEqualTo(1);
        assertThat(((Number) byAgeBand.get("ENFANT")).longValue()).isEqualTo(1);
        assertThat(((Number) byAgeBand.get("NOURRISSON_NOUVEAU_NE")).longValue()).isEqualTo(1);
    }

    @Test
    void inverted_range_is_400() {
        // The from > to guard is uniform across endpoints, not just /activity.
        var http = restClientFor(port, "ADMIN");
        HttpStatus status =
                http.get()
                        .uri("/api/v1/stats/patients?from={f}&to={t}", "2026-05-10", "2026-05-01")
                        .exchange((req, res) -> HttpStatus.valueOf(res.getStatusCode().value()));
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
