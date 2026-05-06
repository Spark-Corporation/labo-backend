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
import org.sparkcorporation.lims.catalog.domain.Technique;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaTechniqueRepositoryTest extends IntegrationTestBase {

    @Autowired private TechniqueRepository repo;

    @Test
    void save_and_find_by_code_round_trip() {
        var t = Technique.create("TECH_TEST", "Tech test", null);
        repo.save(t);
        assertThat(repo.findByCode("TECH_TEST")).isPresent();
    }

    @Test
    void duplicate_code_violates_unique_constraint() {
        repo.saveAndFlush(Technique.create("DUP_TECH_TEST", "Dup", null));
        assertThatThrownBy(
                        () -> repo.saveAndFlush(Technique.create("DUP_TECH_TEST", "Other", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var t = repo.save(Technique.create("SOFT_DEL_TECH_TEST", "Soft", null));
        repo.deleteById(t.rawId());
        repo.flush();
        assertThat(repo.findById(t.rawId())).isPresent();
        assertThat(repo.findActiveById(t.rawId())).isEmpty();
    }

    @Test
    void findAllActive_excludes_soft_deleted() {
        repo.save(Technique.create("ACTIVE_TECH_A", "A", null));
        var t2 = repo.save(Technique.create("ACTIVE_TECH_B", "B", null));
        repo.deleteById(t2.rawId());
        repo.flush();
        var page = repo.findAllActive(PageRequest.of(0, 50));
        assertThat(page.getContent())
                .extracting(Technique::code)
                .contains("ACTIVE_TECH_A")
                .doesNotContain("ACTIVE_TECH_B");
    }

    @Test
    void seeded_techniques_findable_by_code() {
        assertThat(repo.findByCode("ELISA")).isPresent();
        assertThat(repo.findByCode("PCR_RT")).isPresent();
    }
}
