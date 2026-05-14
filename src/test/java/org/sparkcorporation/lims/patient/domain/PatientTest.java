package org.sparkcorporation.lims.patient.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

class PatientTest {

    private static final LabCode A_CODE = new LabCode("LAB-260505-001");

    private static NewPatient minimalCommand() {
        return new NewPatient(
                new PersonName("DOE", "Jane", null),
                Civility.MADAME,
                Sex.F,
                null,
                null,
                null,
                null,
                null,
                new ContactInfo(null, null),
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void create_populates_id_labCode_and_required_fields() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        assertThat(p.id()).isInstanceOf(PatientId.class);
        assertThat(p.labCode()).isEqualTo("LAB-260505-001");
        assertThat(p.civility()).isEqualTo(Civility.MADAME);
        assertThat(p.sex()).isEqualTo(Sex.F);
        assertThat(p.isMerged()).isFalse();
        assertThat(p.isDeleted()).isFalse();
        assertThat(p.insuranceId()).isNull();
        assertThat(p.partnerId()).isNull();
    }

    @Test
    void update_changes_only_non_null_fields() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        var newInsuranceId = Ulid.generate();
        var change =
                new UpdatePatient(
                        null,
                        null,
                        null,
                        null,
                        null,
                        "infirmière",
                        null,
                        null,
                        null,
                        null,
                        newInsuranceId,
                        null,
                        null,
                        null);
        p.update(change);
        assertThat(p.profession()).isEqualTo("infirmière");
        assertThat(p.insuranceId()).isEqualTo(newInsuranceId);
        // unchanged
        assertThat(p.civility()).isEqualTo(Civility.MADAME);
        assertThat(p.address()).isNull();
        assertThat(p.partnerId()).isNull();
    }

    @Test
    void mergeInto_sets_mergedIntoId_once() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        var target = PatientId.next();
        p.mergeInto(target);
        assertThat(p.isMerged()).isTrue();
        assertThat(p.mergedIntoId()).isEqualTo(target.value());
    }

    @Test
    void mergeInto_rejects_double_merge() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        p.mergeInto(PatientId.next());
        assertThatThrownBy(() -> p.mergeInto(PatientId.next()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already merged");
    }

    @Test
    void update_overwrites_every_non_null_field() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        var partnerId = Ulid.generate();
        var insuranceId = Ulid.generate();
        var newName = new PersonName("DUPONT", "Marie", "Anne");
        var newContact = new ContactInfo("+22890000001", "marie@example.test");
        var newGuardian = new Guardian("Father", "+22890000002", GuardianRelationship.PARENT);
        var newIdDoc = new IdDocument(IdDocumentType.CARTE_IDENTITE, "ID-12345");
        var newBirthDate = LocalDate.of(1990, 1, 15);

        var change =
                new UpdatePatient(
                        newName,
                        Civility.MADEMOISELLE,
                        Sex.F,
                        newBirthDate,
                        newIdDoc,
                        "ingénieure",
                        "BP 9000",
                        "diabétique",
                        newContact,
                        newGuardian,
                        insuranceId,
                        partnerId,
                        "marie-login",
                        "hashed-pwd");
        p.update(change);

        assertThat(p.name()).isEqualTo(newName);
        assertThat(p.civility()).isEqualTo(Civility.MADEMOISELLE);
        assertThat(p.sex()).isEqualTo(Sex.F);
        assertThat(p.birthDate()).isEqualTo(newBirthDate);
        assertThat(p.idDocument()).isEqualTo(newIdDoc);
        assertThat(p.profession()).isEqualTo("ingénieure");
        assertThat(p.address()).isEqualTo("BP 9000");
        assertThat(p.clinicalNotes()).isEqualTo("diabétique");
        assertThat(p.contact()).isEqualTo(newContact);
        assertThat(p.guardian()).isEqualTo(newGuardian);
        assertThat(p.insuranceId()).isEqualTo(insuranceId);
        assertThat(p.partnerId()).isEqualTo(partnerId);
        assertThat(p.portalLogin()).isEqualTo("marie-login");
    }

    @Test
    void update_with_all_null_fields_leaves_patient_unchanged() {
        Patient p = Patient.create(minimalCommand(), A_CODE);
        var beforeName = p.name();
        var beforeCivility = p.civility();
        var beforeSex = p.sex();

        var change =
                new UpdatePatient(
                        null, null, null, null, null, null, null, null, null, null, null, null,
                        null, null);
        p.update(change);

        assertThat(p.name()).isEqualTo(beforeName);
        assertThat(p.civility()).isEqualTo(beforeCivility);
        assertThat(p.sex()).isEqualTo(beforeSex);
        assertThat(p.birthDate()).isNull();
        assertThat(p.idDocument()).isNull();
        assertThat(p.profession()).isNull();
        assertThat(p.address()).isNull();
        assertThat(p.clinicalNotes()).isNull();
        assertThat(p.insuranceId()).isNull();
        assertThat(p.partnerId()).isNull();
        assertThat(p.portalLogin()).isNull();
    }

    @Test
    void create_with_full_fields_populates_all_columns() {
        var partnerId = Ulid.generate();
        var insuranceId = Ulid.generate();
        var newName = new PersonName("CISSE", "Aïcha", "Mariam");
        var newContact = new ContactInfo("+22890000010", "aicha@example.test");
        var newGuardian = new Guardian("Aunt", "+22890000011", GuardianRelationship.PARENT);
        var newIdDoc = new IdDocument(IdDocumentType.CARTE_IDENTITE, "ID-99999");
        var newBirthDate = LocalDate.of(1992, 6, 20);
        var cmd =
                new NewPatient(
                        newName,
                        Civility.MADAME,
                        Sex.F,
                        newBirthDate,
                        newIdDoc,
                        "infirmière",
                        "Lomé centre",
                        "RAS",
                        newContact,
                        newGuardian,
                        insuranceId,
                        partnerId,
                        "aicha-login",
                        "$2a$10$dummyhash");

        var p = Patient.create(cmd, A_CODE);

        assertThat(p.name()).isEqualTo(newName);
        assertThat(p.birthDate()).isEqualTo(newBirthDate);
        assertThat(p.idDocument()).isEqualTo(newIdDoc);
        assertThat(p.profession()).isEqualTo("infirmière");
        assertThat(p.address()).isEqualTo("Lomé centre");
        assertThat(p.clinicalNotes()).isEqualTo("RAS");
        assertThat(p.contact()).isEqualTo(newContact);
        assertThat(p.guardian()).isEqualTo(newGuardian);
        assertThat(p.insuranceId()).isEqualTo(insuranceId);
        assertThat(p.partnerId()).isEqualTo(partnerId);
        assertThat(p.portalLogin()).isEqualTo("aicha-login");
        assertThat(p.deletedAt()).isNull();
        assertThat(p.mergedIntoId()).isNull();
    }
}
