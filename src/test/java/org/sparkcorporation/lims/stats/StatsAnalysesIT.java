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

class StatsAnalysesIT extends StatsIntegrationTestBase {

    @LocalServerPort int port;

    @Test
    void counts_resulted_normal_and_high_low_split_with_qualitative_abnormal_edge_case() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(2, ChronoUnit.HOURS);

        String discBio = seedDiscipline("BIO", "Biochimie");
        String aGlu = seedAnalysis("GLU", "Glucose", discBio);
        // STANDARD reference interval [10, 20] for the numeric high/low decision.
        String ri = seedReferenceInterval(aGlu, 10.0, 20.0);

        String patient = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);
        String labFile = seedLabFile(patient, inRange, false, "OPEN", null, null, null);
        String result =
                seedResult(labFile, aGlu, "GLU", "BIOLOGICALLY_VALIDATED", "HOMME", "M", 12000);

        // value=15 in-range (normal); value=25 high; value=5 low (all numeric STANDARD).
        seedResultLine(result, aGlu, "GLU", 15.0, ri, false, 0);
        seedResultLine(result, aGlu, "GLU", 25.0, ri, true, 1);
        seedResultLine(result, aGlu, "GLU", 5.0, ri, true, 2);
        // one out-of-range line with NO reference interval (qualitative): abnormal but not
        // high/low.
        seedResultLine(result, aGlu, "GLU", null, null, true, 3);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/analyses?from={f}&to={t}",
                                today.toString(),
                                today.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        assertThat(rows).hasSize(1);

        Map<String, Object> glu = rows.getFirst();
        assertThat(glu.get("code")).isEqualTo("GLU");
        assertThat(glu.get("name")).isEqualTo("Glucose");
        assertThat(((Number) glu.get("resultedCount")).longValue()).isEqualTo(4);
        assertThat(((Number) glu.get("normalCount")).longValue()).isEqualTo(1);
        assertThat(((Number) glu.get("highCount")).longValue()).isEqualTo(1);
        assertThat(((Number) glu.get("lowCount")).longValue()).isEqualTo(1);
        // The qualitative abnormal contributes to resulted (and abnormal) but not high/low:
        // highCount + lowCount (2) < abnormal (3), by §6.4 design.
    }

    @Test
    void discipline_filter_narrows_to_that_discipline() {
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

        seedResultLine(result, aGlu, "GLU", 5.0, null, false, 0);
        seedResultLine(result, aHb, "HB", 13.0, null, false, 1);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/analyses?from={f}&to={t}&disciplineId={d}",
                                today.toString(),
                                today.toString(),
                                discBio)
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("code")).isEqualTo("GLU");
    }
}
