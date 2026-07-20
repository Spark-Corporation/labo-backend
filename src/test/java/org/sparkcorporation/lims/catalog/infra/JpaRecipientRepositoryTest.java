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
import org.sparkcorporation.lims.catalog.domain.Recipient;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaRecipientRepositoryTest extends IntegrationTestBase {

    @Autowired private RecipientRepository repo;

    private static Recipient sample(String code, String name) {
        return Recipient.create(code, name, null, null, null, null);
    }

    @Test
    void save_and_find_by_code_round_trip() {
        repo.save(sample("REC_TEST", "Rec test"));
        assertThat(repo.findByCode("REC_TEST")).isPresent();
    }

    @Test
    void duplicate_code_violates_unique_constraint() {
        repo.saveAndFlush(sample("DUP_REC_TEST", "Dup"));
        assertThatThrownBy(() -> repo.saveAndFlush(sample("DUP_REC_TEST", "Other")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var r = repo.save(sample("SOFT_DEL_REC_TEST", "Soft"));
        repo.deleteById(r.rawId());
        repo.flush();
        assertThat(repo.findById(r.rawId())).isPresent();
        assertThat(repo.findActiveById(r.rawId())).isEmpty();
    }

    @Test
    void findAllActive_excludes_soft_deleted() {
        repo.save(sample("ACTIVE_REC_A", "A"));
        var r2 = repo.save(sample("ACTIVE_REC_B", "B"));
        repo.deleteById(r2.rawId());
        repo.flush();
        var page = repo.findAllActive(PageRequest.of(0, 50));
        assertThat(page.getContent())
                .extracting(Recipient::code)
                .contains("ACTIVE_REC_A")
                .doesNotContain("ACTIVE_REC_B");
    }

    @Test
    void seeded_recipients_findable_by_code() {
        assertThat(repo.findByCode("TUBE_EDTA")).isPresent();
        assertThat(repo.findByCode("POT_URINE")).isPresent();
    }

    @Test
    void findActiveById_eagerly_loads_defaultMatrix() {
        // Seeded TUBE_EDTA → SANG_TOTAL via Liquibase 1501
        var tube = repo.findByCode("TUBE_EDTA").orElseThrow();
        var fetched = repo.findActiveById(tube.rawId()).orElseThrow();
        assertThat(fetched.defaultMatrix()).isNotNull();
        assertThat(fetched.defaultMatrix().code()).isEqualTo("SANG_TOTAL");
    }
}
