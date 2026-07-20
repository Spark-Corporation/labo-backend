package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.domain.NewSpecialty;
import org.sparkcorporation.lims.catalog.domain.Specialty;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaSpecialtyRepositoryTest extends IntegrationTestBase {

    @Autowired private SpecialtyRepository repo;

    private static Specialty sample(String code, String name) {
        return Specialty.create(new NewSpecialty(code, name, null), Clock.systemUTC());
    }

    @Test
    void save_and_find_by_code_round_trip() {
        repo.save(sample("SPC_TEST", "Specialty test"));
        assertThat(repo.findByCode("SPC_TEST")).isPresent();
    }

    @Test
    void duplicate_code_violates_unique_constraint() {
        repo.saveAndFlush(sample("DUP_SPC_TEST", "Dup"));
        assertThatThrownBy(() -> repo.saveAndFlush(sample("DUP_SPC_TEST", "Other")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var s = repo.save(sample("SOFT_DEL_SPC_TEST", "Soft"));
        repo.deleteById(s.rawId());
        repo.flush();
        // findById returns the row (no @SQLRestriction)
        assertThat(repo.findById(s.rawId())).isPresent();
        // findActiveById filters deleted_at IS NULL
        assertThat(repo.findActiveById(s.rawId())).isEmpty();
    }

    @Test
    void findAllActive_excludes_soft_deleted() {
        repo.save(sample("ACTIVE_SPC_A", "A"));
        var s2 = repo.save(sample("ACTIVE_SPC_B", "B"));
        repo.deleteById(s2.rawId());
        repo.flush();
        assertThat(repo.findAllActive())
                .extracting(Specialty::code)
                .contains("ACTIVE_SPC_A")
                .doesNotContain("ACTIVE_SPC_B");
    }
}
