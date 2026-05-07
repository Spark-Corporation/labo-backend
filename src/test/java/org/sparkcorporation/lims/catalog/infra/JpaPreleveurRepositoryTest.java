package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.api.Habilitation;
import org.sparkcorporation.lims.catalog.domain.NewPreleveur;
import org.sparkcorporation.lims.catalog.domain.Preleveur;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaPreleveurRepositoryTest extends IntegrationTestBase {

    @Autowired private PreleveurRepository repo;

    private static Preleveur newPreleveur(String code, String name, Set<Habilitation> habs) {
        var cmd = new NewPreleveur(name, null, null, null, habs);
        return Preleveur.create(cmd, code, Clock.systemUTC());
    }

    @Test
    void save_and_find_by_code_round_trip() {
        repo.save(newPreleveur("PHL-100001", "Phlebo One", Set.of(Habilitation.TOUT_LE_MONDE)));
        assertThat(repo.findByCode("PHL-100001")).isPresent();
    }

    @Test
    void duplicate_code_violates_unique_constraint() {
        repo.saveAndFlush(newPreleveur("DUP_PHL_TEST", "Dup", Set.of(Habilitation.TOUT_LE_MONDE)));
        assertThatThrownBy(
                        () ->
                                repo.saveAndFlush(
                                        newPreleveur(
                                                "DUP_PHL_TEST",
                                                "Other",
                                                Set.of(Habilitation.TOUT_LE_MONDE))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var p =
                repo.save(
                        newPreleveur(
                                "PHL-100002", "Phlebo Soft", Set.of(Habilitation.TOUT_LE_MONDE)));
        repo.deleteById(p.rawId());
        repo.flush();
        assertThat(repo.findById(p.rawId())).isPresent();
        assertThat(repo.findActiveById(p.rawId())).isEmpty();
    }

    @Test
    void findAllActive_excludes_soft_deleted() {
        repo.save(newPreleveur("PHL-100003", "Active A", Set.of(Habilitation.TOUT_LE_MONDE)));
        var p2 =
                repo.save(
                        newPreleveur("PHL-100004", "Active B", Set.of(Habilitation.TOUT_LE_MONDE)));
        repo.deleteById(p2.rawId());
        repo.flush();
        assertThat(repo.findAllActive())
                .extracting(Preleveur::name)
                .contains("Active A")
                .doesNotContain("Active B");
    }

    @Test
    void persists_3_habilitation_set_round_trip() {
        var habs =
                Set.of(
                        Habilitation.TOUT_LE_MONDE,
                        Habilitation.ADULTES_VEINES_VISIBLES,
                        Habilitation.ENFANTS_VEINES_FINES);
        var p = repo.save(newPreleveur("PHL-100005", "Multi-hab", habs));
        repo.flush();
        var fetched = repo.findById(p.rawId()).orElseThrow();
        assertThat(fetched.habilitations()).hasSize(3).containsExactlyInAnyOrderElementsOf(habs);
    }
}
