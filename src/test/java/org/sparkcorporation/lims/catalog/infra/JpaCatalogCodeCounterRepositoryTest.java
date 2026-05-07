package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * JPA-slice round-trip and pessimistic-lock smoke tests for {@link CatalogCodeCounterRepository}.
 * Counter rows are seeded by Liquibase changeset 202605061600-seed-catalog-code-counters in
 * test/dev/prod contexts.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaCatalogCodeCounterRepositoryTest extends IntegrationTestBase {

    @Autowired private CatalogCodeCounterRepository repo;

    @Test
    void seeded_PRESCRIBER_row_is_findable() {
        assertThat(repo.findById("PRESCRIBER")).isPresent();
    }

    @Test
    void seeded_PRELEVEUR_row_is_findable() {
        assertThat(repo.findById("PRELEVEUR")).isPresent();
    }

    @Test
    @Transactional
    void findByIdForUpdate_returns_seeded_row() {
        var found = repo.findByIdForUpdate("PRESCRIBER");
        assertThat(found).isPresent();
        assertThat(found.get().entityType()).isEqualTo("PRESCRIBER");
    }

    @Test
    @Transactional
    void incrementAndGet_persists_bump() {
        var before = repo.findByIdForUpdate("PRESCRIBER").orElseThrow();
        long start = before.nextSequence();
        long handed = before.incrementAndGet(Clock.systemUTC());
        repo.saveAndFlush(before);

        assertThat(handed).isEqualTo(start);

        var after = repo.findById("PRESCRIBER").orElseThrow();
        assertThat(after.nextSequence()).isEqualTo(start + 1);
    }
}
