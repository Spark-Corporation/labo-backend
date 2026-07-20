package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;

/**
 * Proves the two-block changeset {@code 202605071400-add-discipline-prefix.yaml} applies cleanly on
 * a freshly-migrated database: Block 1 adds {@code discipline_prefix} as nullable, Block 2 tightens
 * to NOT NULL. Since 1.3.a does not seed any disciplines, the backfill is a no-op — verifies that
 * absence rather than asserting any specific data state.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class DisciplinePrefixBackfillIT extends IntegrationTestBase {

    @Autowired private EntityManager em;

    @Test
    void discipline_prefix_column_exists_and_is_not_null() {
        var result =
                em.createNativeQuery(
                                """
                                SELECT is_nullable FROM information_schema.columns
                                 WHERE table_name = 'disciplines' AND column_name = 'discipline_prefix'
                                """)
                        .getSingleResult();
        assertThat(result).isEqualTo("NO"); // NOT NULL constraint applied
    }

    @Test
    void discipline_prefix_column_has_correct_type() {
        var result =
                em.createNativeQuery(
                                """
                                SELECT data_type FROM information_schema.columns
                                 WHERE table_name = 'disciplines' AND column_name = 'discipline_prefix'
                                """)
                        .getSingleResult();
        assertThat(result.toString()).containsIgnoringCase("character varying");
    }
}
