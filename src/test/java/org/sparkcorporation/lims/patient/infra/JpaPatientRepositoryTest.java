package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;

import org.sparkcorporation.lims.IntegrationTestBase;
import org.sparkcorporation.lims.patient.api.PatientApi;
import org.sparkcorporation.lims.patient.api.PatientSummary;
import org.sparkcorporation.lims.patient.domain.ContactInfo;
import org.sparkcorporation.lims.patient.domain.LabCode;
import org.sparkcorporation.lims.patient.domain.NewPatient;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PersonName;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
class JpaPatientRepositoryTest extends IntegrationTestBase {

    @Autowired private PatientRepository repo;
    @Autowired private jakarta.persistence.EntityManager em;

    private static NewPatient cmd(String last, String first) {
        return cmd(last, first, null);
    }

    private static NewPatient cmd(String last, String first, LocalDate birthDate) {
        return new NewPatient(
                new PersonName(last, first, null),
                Civility.MADAME,
                Sex.F,
                birthDate,
                null,
                null,
                null,
                null,
                new ContactInfo(null, null),
                null,
                false,
                null,
                null);
    }

    @Test
    void save_and_find_by_id_round_trip() {
        var p = Patient.create(cmd("DOE", "Jane"), new LabCode("LAB-260505-001"));
        repo.save(p);
        var found = repo.findById(p.id().value());
        assertThat(found).isPresent();
        assertThat(found.get().labCode()).isEqualTo("LAB-260505-001");
    }

    @Test
    void soft_delete_hides_from_findById() {
        var p = Patient.create(cmd("DOE", "Jane"), new LabCode("LAB-260505-002"));
        repo.save(p);
        var rawId = p.id().value();
        repo.deleteById(rawId);
        assertThat(repo.findById(rawId)).isEmpty();
    }

    @Test
    void soft_delete_with_stale_version_throws_OptimisticLockingFailureException() {
        var p = Patient.create(cmd("DOE", "Jane"), new LabCode("LAB-260505-099"));
        repo.saveAndFlush(p);
        em.createNativeQuery("UPDATE patients SET version = version + 1 WHERE id = ?")
                .setParameter(1, p.id().value())
                .executeUpdate();
        assertThatThrownBy(
                        () -> {
                            repo.delete(p);
                            repo.flush();
                        })
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }

    @Test
    void duplicate_lab_code_violates_unique_constraint() {
        var p1 = Patient.create(cmd("DOE", "Jane"), new LabCode("LAB-260505-003"));
        var p2 = Patient.create(cmd("ROE", "Jane"), new LabCode("LAB-260505-003"));
        repo.saveAndFlush(p1);
        assertThatThrownBy(() -> repo.saveAndFlush(p2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void requireById_includes_birthDate_when_present() {
        // Arrange — persist a patient with a known birthDate.
        var birthDate = LocalDate.of(1990, 5, 12);
        var p = Patient.create(cmd("DOE", "Jane", birthDate), new LabCode("LAB-260509-100"));
        repo.saveAndFlush(p);

        // Act — through DefaultPatientApi (the cross-module surface under test).
        PatientApi api = new DefaultPatientApi(repo);
        PatientSummary summary = api.requireById(p.id());

        // Assert
        assertThat(summary.birthDate()).isEqualTo(birthDate);
    }

    @Test
    void requireById_birthDate_is_null_when_absent() {
        var p = Patient.create(cmd("ROE", "John"), new LabCode("LAB-260509-101"));
        repo.saveAndFlush(p);

        PatientApi api = new DefaultPatientApi(repo);
        PatientSummary summary = api.requireById(p.id());

        assertThat(summary.birthDate()).isNull();
    }
}
