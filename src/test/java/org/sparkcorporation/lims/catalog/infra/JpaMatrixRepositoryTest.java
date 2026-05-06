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
import org.sparkcorporation.lims.catalog.domain.Matrix;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaMatrixRepositoryTest extends IntegrationTestBase {

    @Autowired private MatrixRepository repo;

    @Test
    void save_and_find_by_code_round_trip() {
        var m = Matrix.create("MTX_TEST", "Matrix test", null);
        repo.save(m);
        assertThat(repo.findByCode("MTX_TEST")).isPresent();
    }

    @Test
    void duplicate_code_violates_unique_constraint() {
        repo.saveAndFlush(Matrix.create("DUP_MTX_TEST", "Dup", null));
        assertThatThrownBy(() -> repo.saveAndFlush(Matrix.create("DUP_MTX_TEST", "Other", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var m = repo.save(Matrix.create("SOFT_DEL_MTX_TEST", "Soft", null));
        repo.deleteById(m.rawId());
        repo.flush();
        assertThat(repo.findById(m.rawId())).isPresent();
        assertThat(repo.findActiveById(m.rawId())).isEmpty();
    }

    @Test
    void findAllActive_excludes_soft_deleted() {
        repo.save(Matrix.create("ACTIVE_MTX_A", "A", null));
        var m2 = repo.save(Matrix.create("ACTIVE_MTX_B", "B", null));
        repo.deleteById(m2.rawId());
        repo.flush();
        var page = repo.findAllActive(PageRequest.of(0, 50));
        assertThat(page.getContent())
                .extracting(Matrix::code)
                .contains("ACTIVE_MTX_A")
                .doesNotContain("ACTIVE_MTX_B");
    }

    @Test
    void seeded_matrices_findable_by_code() {
        assertThat(repo.findByCode("SANG_TOTAL")).isPresent();
        assertThat(repo.findByCode("SERUM")).isPresent();
    }
}
