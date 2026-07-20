package org.sparkcorporation.lims.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.infra.AnalysisRepository;
import org.sparkcorporation.lims.catalog.infra.DisciplineRepository;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.id.Ulid;

/**
 * Acceptance IT for {@code GET /api/v1/catalog/analyses/{id}/history} (SP12, CDC Configurations
 * §8/§9). A {@code catalog.view-history} holder (ADMIN) gets the paginated timeline; a role without
 * it (CASHIER) gets 403; an unknown analysis id gets 404.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnalysisHistoryControllerIT extends IntegrationTestBase {

    @LocalServerPort private int port;
    @Autowired private AnalysisRepository analyses;
    @Autowired private DisciplineRepository disciplines;
    @Autowired private TransactionTemplate tx;

    @Test
    void admin_gets_paginated_history_with_a_created_entry() {
        AnalysisId aid = seedAnalysis();

        var admin = restClientFor(port, "ADMIN");
        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                admin.get()
                        .uri("/api/v1/catalog/analyses/" + aid.value() + "/history?page=0&size=20")
                        .retrieve()
                        .body(Map.class);

        assertThat(body).isNotNull();
        assertThat(((Number) body.get("totalElements")).longValue()).isGreaterThanOrEqualTo(1L);
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) body.get("content");
        assertThat(content)
                .anySatisfy(
                        e -> {
                            assertThat(e.get("scope")).isEqualTo("ANALYSIS");
                            assertThat(e.get("changedFields").toString()).contains("created");
                        });
    }

    @Test
    void role_without_catalog_view_history_is_forbidden() {
        AnalysisId aid = seedAnalysis();
        var cashier = restClientFor(port, "CASHIER");
        try {
            cashier.get()
                    .uri("/api/v1/catalog/analyses/" + aid.value() + "/history")
                    .retrieve()
                    .toBodilessEntity();
            throw new AssertionError("expected 403 FORBIDDEN");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void unknown_analysis_id_is_not_found() {
        var admin = restClientFor(port, "ADMIN");
        try {
            admin.get()
                    .uri("/api/v1/catalog/analyses/" + Ulid.generate() + "/history")
                    .retrieve()
                    .toBodilessEntity();
            throw new AssertionError("expected 404 NOT_FOUND");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    private AnalysisId seedAnalysis() {
        return tx.execute(
                s -> {
                    String suffix = String.format("%05d", REG_SEQ.getAndIncrement());
                    Discipline d =
                            disciplines.save(
                                    Discipline.create(
                                            "HCTLD" + suffix,
                                            "D" + suffix,
                                            null,
                                            DisciplinePrefix.BIOCHIMIE));
                    NewAnalysis cmd =
                            new NewAnalysis(
                                    "HCTLA" + suffix,
                                    "History controller analysis " + suffix,
                                    AnalysisKind.SIMPLE,
                                    new BigDecimal("10.00"),
                                    1,
                                    d.id(),
                                    null,
                                    null,
                                    ResultType.REAL,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null);
                    Analysis a =
                            analyses.saveAndFlush(
                                    Analysis.create(cmd, d, null, null, Clock.systemUTC()));
                    return a.id();
                });
    }
}
