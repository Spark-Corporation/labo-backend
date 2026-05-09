package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.AnalysisKind;
import org.sparkcorporation.lims.catalog.api.ResultType;
import org.sparkcorporation.lims.catalog.domain.Analysis;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.catalog.domain.Matrix;
import org.sparkcorporation.lims.catalog.domain.NewAnalysis;
import org.sparkcorporation.lims.catalog.domain.Recipient;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaAnalysisRepositoryTest extends IntegrationTestBase {

    @Autowired private AnalysisRepository repo;
    @Autowired private DisciplineRepository disciplines;
    @Autowired private MatrixRepository matrices;
    @Autowired private RecipientRepository recipients;

    private static Analysis sample(
            String code, String name, AnalysisKind kind, Discipline d, Matrix m, Recipient r) {
        var cmd =
                new NewAnalysis(
                        code,
                        name,
                        kind,
                        new BigDecimal("10.00"),
                        1,
                        d.id(),
                        m == null ? null : m.id(),
                        r == null ? null : r.id(),
                        ResultType.REAL,
                        null,
                        null,
                        null,
                        null,
                        null);
        return Analysis.create(cmd, d, m, r, Clock.systemUTC());
    }

    private Discipline freshDiscipline(String code) {
        return disciplines.save(
                Discipline.create(code, code + "_name", null, DisciplinePrefix.BIOCHIMIE));
    }

    @Test
    void save_and_find_by_code_round_trip() {
        var d = freshDiscipline("ANA_DISC_RT");
        repo.save(sample("ANA_RT", "Analysis RT", AnalysisKind.SIMPLE, d, null, null));
        assertThat(repo.findByCode("ANA_RT")).isPresent();
    }

    @Test
    void soft_delete_stamps_deleted_at_and_findActiveById_returns_empty() {
        var d = freshDiscipline("ANA_DISC_SOFT");
        var a = repo.save(sample("ANA_SOFT", "Soft", AnalysisKind.SIMPLE, d, null, null));
        repo.deleteById(a.rawId());
        repo.flush();
        assertThat(repo.findById(a.rawId())).isPresent();
        assertThat(repo.findById(a.rawId()).orElseThrow().deletedAt()).isNotNull();
        assertThat(repo.findActiveById(a.rawId())).isEmpty();
    }

    @Test
    void findActiveById_eagerly_loads_discipline_matrix_recipient() {
        var d = freshDiscipline("ANA_DISC_FETCH");
        var m = matrices.save(Matrix.create("ANA_MTX_FETCH", "Mtx", null));
        var r = recipients.save(Recipient.create("ANA_RCP_FETCH", "Rcp", null, null, null, null));
        var a = repo.save(sample("ANA_FETCH", "Fetch", AnalysisKind.SIMPLE, d, m, r));
        repo.flush();
        var fetched = repo.findActiveById(a.rawId()).orElseThrow();
        assertThat(fetched.discipline()).isNotNull();
        assertThat(fetched.discipline().code()).isEqualTo("ANA_DISC_FETCH");
        assertThat(fetched.matrix()).isNotNull();
        assertThat(fetched.matrix().code()).isEqualTo("ANA_MTX_FETCH");
        assertThat(fetched.recipient()).isNotNull();
        assertThat(fetched.recipient().code()).isEqualTo("ANA_RCP_FETCH");
    }

    @Test
    void findChildrenForParent_returns_components_ordered_by_position() {
        var d = freshDiscipline("ANA_DISC_COMP");
        var parent = repo.save(sample("ANA_PARENT", "Parent", AnalysisKind.GROUPED, d, null, null));
        var childB =
                repo.save(sample("ANA_CHILD_B", "Child B", AnalysisKind.SIMPLE, d, null, null));
        var childA =
                repo.save(sample("ANA_CHILD_A", "Child A", AnalysisKind.SIMPLE, d, null, null));
        // Add in reverse order to verify ordering by position.
        parent.addComponent(childB, 1);
        parent.addComponent(childA, 0);
        repo.saveAndFlush(parent);
        var rows = repo.findChildrenForParent(parent.rawId());
        assertThat(rows).hasSize(2);
        assertThat((String) rows.get(0)[1]).isEqualTo("ANA_CHILD_A"); // position 0 first
        assertThat((String) rows.get(1)[1]).isEqualTo("ANA_CHILD_B"); // position 1 second
    }

    @Test
    void findAllActive_excludes_soft_deleted_ordered_by_code() {
        var d = freshDiscipline("ANA_DISC_LIST");
        repo.save(sample("ANA_AAA", "AAA", AnalysisKind.SIMPLE, d, null, null));
        var b = repo.save(sample("ANA_BBB", "BBB", AnalysisKind.SIMPLE, d, null, null));
        repo.save(sample("ANA_CCC", "CCC", AnalysisKind.SIMPLE, d, null, null));
        repo.deleteById(b.rawId());
        repo.flush();
        assertThat(repo.findAllActive())
                .extracting(Analysis::code)
                .contains("ANA_AAA", "ANA_CCC")
                .doesNotContain("ANA_BBB");
    }

    @Test
    void search_returns_all_active_when_filters_null() {
        var d = freshDiscipline("ANA_DISC_S1");
        repo.save(sample("ANA_S1_A", "Alpha", AnalysisKind.SIMPLE, d, null, null));
        repo.save(sample("ANA_S1_B", "Beta", AnalysisKind.SIMPLE, d, null, null));
        repo.flush();
        assertThat(repo.search(null, null, null))
                .extracting(Analysis::code)
                .contains("ANA_S1_A", "ANA_S1_B");
    }

    @Test
    void search_filters_by_name_substring_case_insensitive() {
        var d = freshDiscipline("ANA_DISC_S2");
        repo.save(sample("ANA_S2_A", "Glucose Plasma", AnalysisKind.SIMPLE, d, null, null));
        repo.save(sample("ANA_S2_B", "Cholesterol", AnalysisKind.SIMPLE, d, null, null));
        repo.flush();
        assertThat(repo.search("glucose", null, null))
                .extracting(Analysis::code)
                .contains("ANA_S2_A")
                .doesNotContain("ANA_S2_B");
    }

    @Test
    void search_filters_by_discipline_code() {
        var biochem = freshDiscipline("ANA_DISC_BIO_S3");
        var hema = freshDiscipline("ANA_DISC_HEM_S3");
        repo.save(sample("ANA_S3_A", "BioOne", AnalysisKind.SIMPLE, biochem, null, null));
        repo.save(sample("ANA_S3_B", "HemOne", AnalysisKind.SIMPLE, hema, null, null));
        repo.flush();
        assertThat(repo.search(null, "ANA_DISC_BIO_S3", null))
                .extracting(Analysis::code)
                .contains("ANA_S3_A")
                .doesNotContain("ANA_S3_B");
    }

    @Test
    void search_filters_by_kind() {
        var d = freshDiscipline("ANA_DISC_S4");
        repo.save(sample("ANA_S4_S", "Simple", AnalysisKind.SIMPLE, d, null, null));
        repo.save(sample("ANA_S4_G", "Grouped", AnalysisKind.GROUPED, d, null, null));
        repo.flush();
        assertThat(repo.search(null, null, AnalysisKind.GROUPED))
                .extracting(Analysis::code)
                .contains("ANA_S4_G")
                .doesNotContain("ANA_S4_S");
    }

    @Test
    void search_combines_all_three_filters() {
        var d1 = freshDiscipline("ANA_DISC_BOTH_S5");
        var d2 = freshDiscipline("ANA_DISC_OTHER_S5");
        repo.save(sample("ANA_S5_A", "Alpha", AnalysisKind.GROUPED, d1, null, null));
        repo.save(sample("ANA_S5_B", "Alpha2", AnalysisKind.SIMPLE, d1, null, null));
        repo.save(sample("ANA_S5_C", "Alpha3", AnalysisKind.GROUPED, d2, null, null));
        repo.flush();
        assertThat(repo.search("alpha", "ANA_DISC_BOTH_S5", AnalysisKind.GROUPED))
                .extracting(Analysis::code)
                .contains("ANA_S5_A")
                .doesNotContain("ANA_S5_B", "ANA_S5_C");
    }

    @Test
    void findActiveByDiscipline_filters_correctly() {
        var d1 = freshDiscipline("ANA_DISC_BD_1");
        var d2 = freshDiscipline("ANA_DISC_BD_2");
        repo.save(sample("ANA_BD_1A", "A", AnalysisKind.SIMPLE, d1, null, null));
        repo.save(sample("ANA_BD_2B", "B", AnalysisKind.SIMPLE, d2, null, null));
        repo.flush();
        assertThat(repo.findActiveByDiscipline(d1.rawId()))
                .extracting(Analysis::code)
                .contains("ANA_BD_1A")
                .doesNotContain("ANA_BD_2B");
    }

    @Test
    void findActiveByDiscipline_excludes_soft_deleted() {
        var d = freshDiscipline("ANA_DISC_BD_SOFT");
        var a = repo.save(sample("ANA_BD_SOFT_A", "A", AnalysisKind.SIMPLE, d, null, null));
        repo.save(sample("ANA_BD_SOFT_B", "B", AnalysisKind.SIMPLE, d, null, null));
        repo.deleteById(a.rawId());
        repo.flush();
        assertThat(repo.findActiveByDiscipline(d.rawId()))
                .extracting(Analysis::code)
                .contains("ANA_BD_SOFT_B")
                .doesNotContain("ANA_BD_SOFT_A");
    }
}
