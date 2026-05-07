package org.sparkcorporation.lims.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.infra.AntibioticRepository;
import org.sparkcorporation.lims.catalog.infra.CatalogCodeCounterRepository;
import org.sparkcorporation.lims.catalog.infra.DisciplineRepository;
import org.sparkcorporation.lims.catalog.infra.MatrixRepository;
import org.sparkcorporation.lims.catalog.infra.RecipientRepository;
import org.sparkcorporation.lims.catalog.infra.TechniqueRepository;

@Tag("integration")
@SpringBootTest
class CatalogSeedIntegrityTest extends IntegrationTestBase {

    @Autowired private DisciplineRepository disciplines;
    @Autowired private MatrixRepository matrices;
    @Autowired private TechniqueRepository techniques;
    @Autowired private RecipientRepository recipients;
    @Autowired private AntibioticRepository antibiotics;
    @Autowired private CatalogCodeCounterRepository counters;

    @Test
    void all_seeded_codes_present_after_liquibase_apply() {
        // Spec §4.4 + §4.5: 5 + 10 + 10 + 8 + 15 = 48 active rows total.
        // Use findAllActiveOrderByCode() — repo.count() would include soft-deleted.
        assertThat(disciplines.findAllActiveOrderByCode()).hasSize(5);
        assertThat(matrices.findAllActiveOrderByCode()).hasSize(10);
        assertThat(techniques.findAllActiveOrderByCode()).hasSize(10);
        assertThat(recipients.findAllActiveOrderByCode()).hasSize(8);
        assertThat(antibiotics.findAllActiveOrderByCode()).hasSize(15);

        // Sample spot-checks across all 5 entity types
        assertThat(disciplines.findByCode("BIOCHIMIE")).isPresent();
        assertThat(matrices.findByCode("SANG_TOTAL")).isPresent();
        assertThat(techniques.findByCode("ELISA")).isPresent();
        assertThat(recipients.findByCode("TUBE_EDTA")).isPresent();
        assertThat(antibiotics.findByCode("AMX")).isPresent();
    }

    @Test
    void seeded_TUBE_EDTA_resolves_to_seeded_SANG_TOTAL_matrix() {
        // Verifies the FK wiring inside changeset 1501 (recipients.default_matrix_id ->
        // matrices.id)
        var tube = recipients.findByCode("TUBE_EDTA").orElseThrow();
        var fetched = recipients.findActiveById(tube.rawId()).orElseThrow();
        assertThat(fetched.defaultMatrix()).isNotNull();
        assertThat(fetched.defaultMatrix().code()).isEqualTo("SANG_TOTAL");
    }

    @Test
    void seeded_catalog_code_counter_rows_present_for_PRESCRIBER_and_PRELEVEUR() {
        var prescriber = counters.findById("PRESCRIBER").orElseThrow();
        assertThat(prescriber.nextSequence()).isGreaterThanOrEqualTo(1L);

        var preleveur = counters.findById("PRELEVEUR").orElseThrow();
        assertThat(preleveur.nextSequence()).isGreaterThanOrEqualTo(1L);
    }
}
