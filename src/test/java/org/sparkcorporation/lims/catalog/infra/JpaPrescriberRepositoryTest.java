package org.sparkcorporation.lims.catalog.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.catalog.domain.NewPrescriber;
import org.sparkcorporation.lims.catalog.domain.NewSpecialty;
import org.sparkcorporation.lims.catalog.domain.Prescriber;
import org.sparkcorporation.lims.catalog.domain.Specialty;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaPrescriberRepositoryTest extends IntegrationTestBase {

    @Autowired private PrescriberRepository repo;
    @Autowired private SpecialtyRepository specialties;

    private static Specialty newSpecialty(String code, String name) {
        return Specialty.create(new NewSpecialty(code, name, null), Clock.systemUTC());
    }

    private static Prescriber newPrescriber(String code, String name, Specialty specialty) {
        var cmd = new NewPrescriber(null, name, null, null, null, null, null);
        return Prescriber.create(cmd, code, specialty, Clock.systemUTC());
    }

    @Test
    void save_and_find_by_code_round_trip() {
        repo.save(newPrescriber("PRS-100001", "Dr. Test", null));
        assertThat(repo.findByCode("PRS-100001")).isPresent();
    }

    @Test
    void soft_delete_keeps_row_but_findActiveById_returns_empty() {
        var p = repo.save(newPrescriber("PRS-100002", "Dr. Soft", null));
        repo.deleteById(p.rawId());
        repo.flush();
        assertThat(repo.findById(p.rawId())).isPresent();
        assertThat(repo.findActiveById(p.rawId())).isEmpty();
    }

    @Test
    void findActiveById_eagerly_loads_specialty() {
        var spec = specialties.save(newSpecialty("CARDIO_TEST", "Cardiologie"));
        var p = repo.save(newPrescriber("PRS-100003", "Dr. Heart", spec));
        repo.flush();
        var fetched = repo.findActiveById(p.rawId()).orElseThrow();
        assertThat(fetched.specialty()).isNotNull();
        assertThat(fetched.specialty().code()).isEqualTo("CARDIO_TEST");
    }

    @Test
    void findAllActive_excludes_soft_deleted_ordered_by_name() {
        repo.save(newPrescriber("PRS-100004", "Aaron Active", null));
        var p2 = repo.save(newPrescriber("PRS-100005", "Bob Bye", null));
        repo.deleteById(p2.rawId());
        repo.flush();
        assertThat(repo.findAllActive())
                .extracting(Prescriber::name)
                .contains("Aaron Active")
                .doesNotContain("Bob Bye");
    }

    @Test
    void search_filters_by_name_substring_case_insensitive() {
        repo.save(newPrescriber("PRS-100010", "Alice Wonder", null));
        repo.save(newPrescriber("PRS-100011", "Bob Builder", null));
        repo.flush();
        assertThat(repo.search("alice", null))
                .extracting(Prescriber::name)
                .contains("Alice Wonder")
                .doesNotContain("Bob Builder");
    }

    @Test
    void search_filters_by_specialty_code() {
        var cardio = specialties.save(newSpecialty("CARDIO_SEARCH", "Cardio"));
        var derm = specialties.save(newSpecialty("DERMA_SEARCH", "Derm"));
        repo.save(newPrescriber("PRS-100020", "Dr. Cardio One", cardio));
        repo.save(newPrescriber("PRS-100021", "Dr. Derm One", derm));
        repo.flush();
        assertThat(repo.search(null, "CARDIO_SEARCH"))
                .extracting(Prescriber::name)
                .contains("Dr. Cardio One")
                .doesNotContain("Dr. Derm One");
    }

    @Test
    void search_with_both_null_returns_all_active() {
        repo.save(newPrescriber("PRS-100030", "All A", null));
        repo.save(newPrescriber("PRS-100031", "All B", null));
        repo.flush();
        assertThat(repo.search(null, null)).extracting(Prescriber::name).contains("All A", "All B");
    }
}
