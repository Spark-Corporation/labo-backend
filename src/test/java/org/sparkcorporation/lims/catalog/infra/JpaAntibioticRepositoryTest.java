package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.domain.Antibiotic;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaAntibioticRepositoryTest extends IntegrationTestBase {

    @Autowired private AntibioticRepository repo;

    private static Antibiotic sample(String code, String name) {
        return Antibiotic.create(code, name, null, null, new BigDecimal("18.00"), null, null);
    }

    @Test
    void save_and_find_by_code_round_trip() {
        repo.save(sample("ABX_TEST", "Abx test"));
        assertThat(repo.findByCode("ABX_TEST")).isPresent();
    }

    @Test
    void duplicate_code_violates_unique_constraint() {
        repo.saveAndFlush(sample("DUP_ABX_TEST", "Dup"));
        assertThatThrownBy(() -> repo.saveAndFlush(sample("DUP_ABX_TEST", "Other")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var a = repo.save(sample("SOFT_DEL_ABX_TEST", "Soft"));
        repo.deleteById(a.rawId());
        repo.flush();
        assertThat(repo.findById(a.rawId())).isPresent();
        assertThat(repo.findActiveById(a.rawId())).isEmpty();
    }

    @Test
    void findAllActive_excludes_soft_deleted() {
        repo.save(sample("ACTIVE_ABX_A", "A"));
        var a2 = repo.save(sample("ACTIVE_ABX_B", "B"));
        repo.deleteById(a2.rawId());
        repo.flush();
        var page = repo.findAllActive(PageRequest.of(0, 50));
        assertThat(page.getContent())
                .extracting(Antibiotic::code)
                .contains("ACTIVE_ABX_A")
                .doesNotContain("ACTIVE_ABX_B");
    }

    @Test
    void seeded_antibiotics_findable_by_code() {
        assertThat(repo.findByCode("AMX")).isPresent();
        assertThat(repo.findByCode("CIP")).isPresent();
    }
}
