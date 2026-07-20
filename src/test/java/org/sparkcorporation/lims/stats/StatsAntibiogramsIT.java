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

class StatsAntibiogramsIT extends StatsIntegrationTestBase {

    @LocalServerPort int port;

    @Test
    void counts_sir_buckets_per_antibiotic() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(2, ChronoUnit.HOURS);

        String discMicro = seedDiscipline("MICRO", "Microbiologie");
        String aCulture = seedAnalysis("CULT", "Culture", discMicro);
        String amox = seedAntibiotic("AMOX", "Amoxicilline");

        String patient = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);
        String labFile = seedLabFile(patient, inRange, false, "OPEN", null, null, null);
        String result =
                seedResult(
                        labFile, aCulture, "CULT", "BIOLOGICALLY_VALIDATED", "HOMME", "M", 12000);

        // 2×'S', 1×'I', 3×'R' for the single antibiotic.
        seedAntibiogramReading(result, amox, 'S', 0);
        seedAntibiogramReading(result, amox, 'S', 1);
        seedAntibiogramReading(result, amox, 'I', 2);
        seedAntibiogramReading(result, amox, 'R', 3);
        seedAntibiogramReading(result, amox, 'R', 4);
        seedAntibiogramReading(result, amox, 'R', 5);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/antibiograms?from={f}&to={t}",
                                today.toString(),
                                today.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        assertThat(rows).hasSize(1);

        Map<String, Object> row = rows.getFirst();
        assertThat(row.get("antibioticName")).isEqualTo("Amoxicilline");
        assertThat(((Number) row.get("sensible")).longValue()).isEqualTo(2);
        assertThat(((Number) row.get("intermediate")).longValue()).isEqualTo(1);
        assertThat(((Number) row.get("resistant")).longValue()).isEqualTo(3);
    }

    @Test
    void discipline_filter_narrows_by_result_analysis_discipline() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(2, ChronoUnit.HOURS);

        String discMicro = seedDiscipline("MICRO", "Microbiologie");
        String discBio = seedDiscipline("BIO", "Biochimie");
        String aCulture = seedAnalysis("CULT", "Culture", discMicro);
        String amox = seedAntibiotic("AMOX", "Amoxicilline");

        String patient = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);
        String labFile = seedLabFile(patient, inRange, false, "OPEN", null, null, null);
        String result =
                seedResult(
                        labFile, aCulture, "CULT", "BIOLOGICALLY_VALIDATED", "HOMME", "M", 12000);
        seedAntibiogramReading(result, amox, 'S', 0);

        var http = restClientFor(port, "ADMIN");
        // Filtering by the microbiology discipline keeps the reading.
        @SuppressWarnings("unchecked")
        Map<String, Object> microBody =
                http.get()
                        .uri(
                                "/api/v1/stats/antibiograms?from={f}&to={t}&disciplineId={d}",
                                today.toString(),
                                today.toString(),
                                discMicro)
                        .retrieve()
                        .body(Map.class);
        assertThat(microBody).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> microRows = (List<Map<String, Object>>) microBody.get("rows");
        assertThat(microRows).hasSize(1);

        // Filtering by an unrelated discipline drops it.
        @SuppressWarnings("unchecked")
        Map<String, Object> bioBody =
                http.get()
                        .uri(
                                "/api/v1/stats/antibiograms?from={f}&to={t}&disciplineId={d}",
                                today.toString(),
                                today.toString(),
                                discBio)
                        .retrieve()
                        .body(Map.class);
        assertThat(bioBody).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bioRows = (List<Map<String, Object>>) bioBody.get("rows");
        assertThat(bioRows).isEmpty();
    }
}
