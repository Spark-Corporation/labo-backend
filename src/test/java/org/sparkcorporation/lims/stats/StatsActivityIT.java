package org.sparkcorporation.lims.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

class StatsActivityIT extends StatsIntegrationTestBase {

    @LocalServerPort int port;

    @Test
    void activity_counts_dossiers_and_new_vs_returning_patients() {
        LocalDate today = LocalDate.now();
        Instant inRange =
                today.atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .plus(2, ChronoUnit.HOURS);
        // a "new" patient created today, with a dossier today
        String pNew = seedPatient("M", LocalDate.of(1990, 1, 1), inRange);
        seedLabFile(pNew, inRange, false, "OPEN", null, null, null);
        // a "returning" patient created before the range, dossier today
        String pOld =
                seedPatient("F", LocalDate.of(1980, 1, 1), inRange.minus(40, ChronoUnit.DAYS));
        seedLabFile(pOld, inRange, true, "BIOLOGICALLY_VALIDATED", null, null, null);

        var http = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                http.get()
                        .uri(
                                "/api/v1/stats/activity?from={f}&to={t}",
                                today.toString(),
                                today.toString())
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        assertThat(((Number) body.get("dossiers")).longValue()).isEqualTo(2);
        assertThat(((Number) body.get("urgents")).longValue()).isEqualTo(1);
        assertThat(((Number) body.get("validated")).longValue()).isEqualTo(1);
        assertThat(((Number) body.get("patientsAll")).longValue()).isEqualTo(2);
        assertThat(((Number) body.get("patientsNew")).longValue()).isEqualTo(1);
        assertThat(((Number) body.get("patientsReturning")).longValue()).isEqualTo(1);
    }

    @Test
    void inverted_range_is_400() {
        var http = restClientFor(port, "ADMIN");
        HttpStatus status =
                http.get()
                        .uri("/api/v1/stats/activity?from={f}&to={t}", "2026-05-10", "2026-05-01")
                        .exchange((req, res) -> HttpStatus.valueOf(res.getStatusCode().value()));
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void missing_permission_is_403() {
        // TECHNICIAN is NOT granted stats.read (only ADMIN/LAB_MANAGER/BIOLOGIST/ACCOUNTANT are).
        var http = restClientFor(port, "TECHNICIAN");
        HttpStatus status =
                http.get()
                        .uri("/api/v1/stats/activity?from={f}&to={t}", "2026-05-01", "2026-05-10")
                        .exchange((req, res) -> HttpStatus.valueOf(res.getStatusCode().value()));
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
