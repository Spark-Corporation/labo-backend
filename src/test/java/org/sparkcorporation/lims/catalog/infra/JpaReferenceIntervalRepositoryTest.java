package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.AnalysisId;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ReferenceMode;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.domain.NewReferenceInterval;
import org.sparkcorporation.lims.catalog.domain.ReferenceInterval;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;
import org.sparkcorporation.lims.shared.domain.PatientCategory;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaReferenceIntervalRepositoryTest extends IntegrationTestBase {

    @Autowired private ReferenceIntervalRepository repo;
    @Autowired private AnalysisRepository analyses;
    @Autowired private DisciplineRepository disciplines;

    private AnalysisId persistAnalysis(String suffix) {
        var d =
                disciplines.save(
                        Discipline.create(
                                "RI_DISC_" + suffix,
                                "D" + suffix,
                                null,
                                DisciplinePrefix.BIOCHIMIE));
        var cmd =
                new NewAnalysis(
                        "RI_ANA_" + suffix,
                        "Analysis " + suffix,
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
                        null);
        var a = analyses.save(Analysis.create(cmd, d, null, null, Clock.systemUTC()));
        analyses.flush();
        return a.id();
    }

    private static ReferenceInterval standard(
            AnalysisId aid, PatientCategory cat, BigDecimal min, BigDecimal max, Integer ageMin) {
        var cmd =
                new NewReferenceInterval(
                        cat,
                        null,
                        ageMin,
                        null,
                        null,
                        ReferenceMode.STANDARD,
                        min,
                        max,
                        null,
                        null,
                        null);
        return ReferenceInterval.create(cmd, aid, Clock.systemUTC());
    }

    @Test
    void save_and_round_trip() {
        var aid = persistAnalysis("RT");
        var ri =
                standard(
                        aid,
                        PatientCategory.HOMME,
                        new BigDecimal("0.5"),
                        new BigDecimal("1.5"),
                        null);
        repo.save(ri);
        repo.flush();
        assertThat(repo.findActiveByAnalysisId(aid.value())).hasSize(1);
    }

    @Test
    void findActiveByAnalysisId_orders_by_category_then_ageMin_nulls_first() {
        var aid = persistAnalysis("ORDER");
        repo.save(
                standard(aid, PatientCategory.HOMME, new BigDecimal("1"), new BigDecimal("2"), 10));
        repo.save(
                standard(
                        aid,
                        PatientCategory.HOMME,
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        null));
        repo.save(
                standard(aid, PatientCategory.FEMME, new BigDecimal("1"), new BigDecimal("2"), 5));
        repo.flush();

        var rows = repo.findActiveByAnalysisId(aid.value());
        assertThat(rows).hasSize(3);
        // ordering: category alphabetical (ENFANT < FEMME < HOMME < NOURRISSON_NOUVEAU_NE).
        assertThat(rows.get(0).category()).isEqualTo(PatientCategory.FEMME);
        assertThat(rows.get(1).category()).isEqualTo(PatientCategory.HOMME);
        assertThat(rows.get(1).ageMin()).isNull();
        assertThat(rows.get(2).category()).isEqualTo(PatientCategory.HOMME);
        assertThat(rows.get(2).ageMin()).isEqualTo(10);
    }

    @Test
    void findActiveByAnalysisIdAndCategory_filters() {
        var aid = persistAnalysis("BYCAT");
        repo.save(
                standard(
                        aid,
                        PatientCategory.HOMME,
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        null));
        repo.save(
                standard(
                        aid,
                        PatientCategory.FEMME,
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        null));
        repo.flush();
        assertThat(repo.findActiveByAnalysisIdAndCategory(aid.value(), PatientCategory.HOMME))
                .hasSize(1)
                .allSatisfy(ri -> assertThat(ri.category()).isEqualTo(PatientCategory.HOMME));
    }

    @Test
    void findActive_excludes_soft_deleted() {
        var aid = persistAnalysis("SOFT");
        var ri =
                repo.save(
                        standard(
                                aid,
                                PatientCategory.HOMME,
                                new BigDecimal("1"),
                                new BigDecimal("2"),
                                null));
        repo.flush();
        repo.deleteById(ri.rawId());
        repo.flush();
        assertThat(repo.findActiveByAnalysisId(aid.value())).isEmpty();
    }

    @Test
    void softDeleteByAnalysisIdAndCategoryIn_affects_only_matching_categories() {
        var aid = persistAnalysis("BULK");
        repo.save(
                standard(
                        aid,
                        PatientCategory.HOMME,
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        null));
        repo.save(
                standard(
                        aid,
                        PatientCategory.FEMME,
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        null));
        repo.save(
                standard(
                        aid,
                        PatientCategory.ENFANT,
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        null));
        repo.flush();

        int count =
                repo.softDeleteByAnalysisIdAndCategoryIn(
                        aid.value(), Set.of(PatientCategory.HOMME, PatientCategory.FEMME));
        repo.flush();

        assertThat(count).isEqualTo(2);
        assertThat(repo.findActiveByAnalysisId(aid.value()))
                .hasSize(1)
                .allSatisfy(ri -> assertThat(ri.category()).isEqualTo(PatientCategory.ENFANT));
    }

    @Test
    void softDeleteByAnalysisIdAndCategoryIn_returns_zero_when_none_match() {
        var aid = persistAnalysis("ZERO");
        repo.save(
                standard(
                        aid,
                        PatientCategory.HOMME,
                        new BigDecimal("1"),
                        new BigDecimal("2"),
                        null));
        repo.flush();
        int count =
                repo.softDeleteByAnalysisIdAndCategoryIn(
                        aid.value(), Set.of(PatientCategory.NOURRISSON_NOUVEAU_NE));
        assertThat(count).isZero();
    }
}
