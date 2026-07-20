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

class StatsByPrescriberIT extends StatsIntegrationTestBase {

    @LocalServerPort int port;

    @Test
    void counts_dossiers_per_prescriber_and_excludes_prescriber_less_dossiers() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(2, ChronoUnit.HOURS);

        String prA = seedPrescriber("DR-A", "Docteur Alpha");
        String prB = seedPrescriber("DR-B", "Docteur Beta");

        String patient = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);
        // prA: two dossiers; prB: one dossier.
        seedLabFile(patient, inRange, false, "OPEN", null, null, prA);
        seedLabFile(patient, inRange, false, "OPEN", null, null, prA);
        seedLabFile(patient, inRange, false, "OPEN", null, null, prB);
        // a dossier with NO prescriber — deliberately excluded from every prescriber's count.
        seedLabFile(patient, inRange, false, "OPEN", null, null, null);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/by-prescriber?from={f}&to={t}",
                                today.toString(),
                                today.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        // Only prA and prB appear; the prescriber-less dossier is not attributed to anyone.
        assertThat(rows).hasSize(2);
        long totalAttributed =
                rows.stream().mapToLong(r -> ((Number) r.get("dossierCount")).longValue()).sum();
        assertThat(totalAttributed).isEqualTo(3);

        Map<String, Object> alpha =
                rows.stream()
                        .filter(r -> "Docteur Alpha".equals(r.get("prescriberName")))
                        .findFirst()
                        .orElseThrow();
        assertThat(((Number) alpha.get("dossierCount")).longValue()).isEqualTo(2);

        Map<String, Object> beta =
                rows.stream()
                        .filter(r -> "Docteur Beta".equals(r.get("prescriberName")))
                        .findFirst()
                        .orElseThrow();
        assertThat(((Number) beta.get("dossierCount")).longValue()).isEqualTo(1);
    }

    @Test
    void discipline_filter_only_counts_dossiers_with_an_analysis_in_that_discipline() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(2, ChronoUnit.HOURS);

        String discBio = seedDiscipline("BIO", "Biochimie");
        String discHema = seedDiscipline("HEMA", "Hematologie");
        String aGlu = seedAnalysis("GLU", "Glucose", discBio);
        String aHb = seedAnalysis("HB", "Hemoglobine", discHema);

        String pr = seedPrescriber("DR-A", "Docteur Alpha");
        String patient = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);

        // Dossier 1: has a biochimie analysis request -> matches the BIO filter.
        String lf1 = seedLabFile(patient, inRange, false, "OPEN", null, null, pr);
        seedResult(lf1, aGlu, "GLU", "BIOLOGICALLY_VALIDATED", "HOMME", "M", 12000);
        // Dossier 2: has only a hematologie analysis request -> excluded by the BIO filter.
        String lf2 = seedLabFile(patient, inRange, false, "OPEN", null, null, pr);
        seedResult(lf2, aHb, "HB", "BIOLOGICALLY_VALIDATED", "HOMME", "M", 12000);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/by-prescriber?from={f}&to={t}&disciplineId={d}",
                                today.toString(),
                                today.toString(),
                                discBio)
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("dossierCount")).longValue()).isEqualTo(1);
        assertThat(rows.get(0).get("prescriberName")).isEqualTo("Docteur Alpha");
    }
}
