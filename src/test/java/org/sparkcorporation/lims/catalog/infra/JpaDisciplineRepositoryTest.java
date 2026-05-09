package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.domain.Discipline;
import org.sparkcorporation.lims.shared.domain.DisciplinePrefix;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaDisciplineRepositoryTest extends IntegrationTestBase {

    @Autowired private DisciplineRepository repo;

    @Test
    void save_and_find_by_code_round_trip() {
        var d = Discipline.create("CHIMIE_TEST", "Chimie Test", null, DisciplinePrefix.BIOCHIMIE);
        repo.save(d);
        assertThat(repo.findByCode("CHIMIE_TEST")).isPresent();
    }

    @Test
    void duplicate_code_violates_unique_constraint() {
        repo.saveAndFlush(Discipline.create("DUP_TEST", "Dup", null, DisciplinePrefix.BIOCHIMIE));
        assertThatThrownBy(
                        () ->
                                repo.saveAndFlush(
                                        Discipline.create(
                                                "DUP_TEST",
                                                "Other",
                                                null,
                                                DisciplinePrefix.BIOCHIMIE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var d =
                repo.save(
                        Discipline.create(
                                "SOFT_DEL_TEST", "Soft", null, DisciplinePrefix.BIOCHIMIE));
        repo.deleteById(d.rawId());
        repo.flush();
        // findById returns the row (no @SQLRestriction)
        assertThat(repo.findById(d.rawId())).isPresent();
        // findActiveById filters deleted_at IS NULL
        assertThat(repo.findActiveById(d.rawId())).isEmpty();
    }

    @Test
    void findAllActive_excludes_soft_deleted() {
        repo.save(Discipline.create("ACTIVE_A", "A", null, DisciplinePrefix.BIOCHIMIE));
        var d2 = repo.save(Discipline.create("ACTIVE_B", "B", null, DisciplinePrefix.BIOCHIMIE));
        repo.deleteById(d2.rawId());
        repo.flush();
        var page = repo.findAllActive(PageRequest.of(0, 50));
        assertThat(page.getContent())
                .extracting(Discipline::code)
                .contains("ACTIVE_A")
                .doesNotContain("ACTIVE_B");
    }

    @Test
    void seeded_disciplines_findable_by_code() {
        // Liquibase data changeset 1501 seeds 5 disciplines in context=test
        // (application-test.yml has spring.liquibase.contexts: test)
        assertThat(repo.findByCode("BIOCHIMIE")).isPresent();
        assertThat(repo.findByCode("HEMATOLOGIE")).isPresent();
    }
}
