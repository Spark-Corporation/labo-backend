package org.sparkcorporation.lims.patient.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.sparkcorporation.lims.catalog.api.InsuranceApi;
import org.sparkcorporation.lims.catalog.api.InsuranceId;
import org.sparkcorporation.lims.catalog.api.InsuranceSummary;
import org.sparkcorporation.lims.catalog.api.PartnerApi;
import org.sparkcorporation.lims.catalog.api.PartnerId;
import org.sparkcorporation.lims.catalog.api.PartnerKind;
import org.sparkcorporation.lims.catalog.api.PartnerSummary;
import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.domain.ContactInfo;
import org.sparkcorporation.lims.patient.domain.Guardian;
import org.sparkcorporation.lims.patient.domain.GuardianRelationship;
import org.sparkcorporation.lims.patient.domain.LabCode;
import org.sparkcorporation.lims.patient.domain.NewPatient;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PatientNotFoundException;
import org.sparkcorporation.lims.patient.domain.PersonName;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;
import org.sparkcorporation.lims.shared.id.Ulid;

class DefaultPatientApiTest {

    private final PatientRepository repo = mock(PatientRepository.class);
    private final InsuranceApi insuranceApi = mock(InsuranceApi.class);
    private final PartnerApi partnerApi = mock(PartnerApi.class);
    private final DefaultPatientApi api = new DefaultPatientApi(repo, insuranceApi, partnerApi);

    private static Patient sample() {
        return Patient.create(
                new NewPatient(
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
                        null),
                new LabCode("LAB-260505-001"));
    }

    private static Patient sampleWithRefs(String insuranceId, String partnerId) {
        return Patient.create(
                new NewPatient(
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
                        insuranceId,
                        partnerId,
                        null,
                        null),
                new LabCode("LAB-260505-002"));
    }

    @Test
    void findById_returns_summary_when_present() {
        Patient p = sample();
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));

        var summary = api.findById(p.id());
        assertThat(summary).isPresent();
        assertThat(summary.get().labCode()).isEqualTo("LAB-260505-001");
        assertThat(summary.get().lastName()).isEqualTo("DOE");
        assertThat(summary.get().firstNames()).isEqualTo("Jane");
        assertThat(summary.get().merged()).isFalse();
        assertThat(summary.get().insurance()).isNull();
        assertThat(summary.get().partner()).isNull();
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThat(api.findById(PatientId.next())).isEmpty();
    }

    @Test
    void requireById_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.requireById(PatientId.next()))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void followMergeChain_returns_target_when_merged() {
        Patient p = sample();
        var target = PatientId.next();
        p.mergeInto(target);
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));
        assertThat(api.followMergeChain(p.id())).isEqualTo(target);
    }

    @Test
    void followMergeChain_returns_self_when_not_merged() {
        Patient p = sample();
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));
        assertThat(api.followMergeChain(p.id())).isEqualTo(p.id());
    }

    @Test
    void findById_resolves_insurance_snapshot_when_insurance_id_present() {
        var insuranceId = Ulid.generate();
        Patient p = sampleWithRefs(insuranceId, null);
        var ins =
                new InsuranceSummary(
                        new InsuranceId(insuranceId),
                        "INS",
                        "Sample",
                        null,
                        null,
                        new BigDecimal("400.00"),
                        new BigDecimal("0.80"),
                        false);
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));
        when(insuranceApi.findById(new InsuranceId(insuranceId))).thenReturn(Optional.of(ins));

        var summary = api.findById(p.id()).orElseThrow();
        assertThat(summary.insurance()).isNotNull();
        assertThat(summary.insurance().code()).isEqualTo("INS");
        assertThat(summary.partner()).isNull();
    }

    @Test
    void findById_renders_null_insurance_when_catalog_returns_empty_soft_deleted() {
        var insuranceId = Ulid.generate();
        Patient p = sampleWithRefs(insuranceId, null);
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));
        when(insuranceApi.findById(any())).thenReturn(Optional.empty());

        var summary = api.findById(p.id()).orElseThrow();
        assertThat(summary.insurance()).isNull();
    }

    private static Patient sampleWithContact(ContactInfo contact) {
        return Patient.create(
                new NewPatient(
                        new PersonName("DOE", "Jane", null),
                        Civility.MADAME,
                        Sex.F,
                        null,
                        null,
                        null,
                        null,
                        null,
                        contact,
                        null,
                        null,
                        null,
                        null,
                        null),
                new LabCode("LAB-260505-003"));
    }

    private static Patient sampleWithContactAndGuardian(ContactInfo contact, Guardian guardian) {
        return Patient.create(
                new NewPatient(
                        new PersonName("DOE", "Jane", null),
                        Civility.MADAME,
                        Sex.F,
                        null,
                        null,
                        null,
                        null,
                        null,
                        contact,
                        guardian,
                        null,
                        null,
                        null,
                        null),
                new LabCode("LAB-260505-004"));
    }

    @Test
    void contactFor_maps_email_and_phone_and_guardian_phone() {
        Patient p =
                sampleWithContactAndGuardian(
                        new ContactInfo("+22890000000", "jane@doe.test"),
                        new Guardian("Tutor", "+22899999999", GuardianRelationship.PARENT));
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));

        var contact = api.contactFor(p.id());
        assertThat(contact.email()).isEqualTo("jane@doe.test");
        assertThat(contact.phone()).isEqualTo("+22890000000");
        assertThat(contact.guardianPhone()).isEqualTo("+22899999999");
    }

    @Test
    void contactFor_preserves_nulls_when_fields_absent() {
        Patient p = sampleWithContact(new ContactInfo(null, null));
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));

        var contact = api.contactFor(p.id());
        assertThat(contact.email()).isNull();
        assertThat(contact.phone()).isNull();
        assertThat(contact.guardianPhone()).isNull();
    }

    @Test
    void contactFor_throws_when_missing() {
        when(repo.findById(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> api.contactFor(PatientId.next()))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void findById_resolves_partner_snapshot_when_partner_id_present() {
        var partnerId = Ulid.generate();
        Patient p = sampleWithRefs(null, partnerId);
        var part =
                new PartnerSummary(
                        new PartnerId(partnerId),
                        "PRT",
                        "Sample Partner",
                        PartnerKind.SOCIETY,
                        null,
                        null,
                        new BigDecimal("300.00"),
                        false);
        when(repo.findById(p.id().value())).thenReturn(Optional.of(p));
        when(partnerApi.findById(new PartnerId(partnerId))).thenReturn(Optional.of(part));

        var summary = api.findById(p.id()).orElseThrow();
        assertThat(summary.partner()).isNotNull();
        assertThat(summary.partner().code()).isEqualTo("PRT");
        assertThat(summary.partner().kind()).isEqualTo(PartnerKind.SOCIETY);
    }
}
