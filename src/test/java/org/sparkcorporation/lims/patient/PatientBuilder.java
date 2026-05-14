package org.sparkcorporation.lims.patient;

import java.time.LocalDate;

import org.sparkcorporation.lims.patient.api.PatientId;
import org.sparkcorporation.lims.patient.domain.ContactInfo;
import org.sparkcorporation.lims.patient.domain.LabCode;
import org.sparkcorporation.lims.patient.domain.NewPatient;
import org.sparkcorporation.lims.patient.domain.Patient;
import org.sparkcorporation.lims.patient.domain.PersonName;
import org.sparkcorporation.lims.patient.web.CreatePatientRequest;
import org.sparkcorporation.lims.shared.domain.Civility;
import org.sparkcorporation.lims.shared.domain.Sex;

public final class PatientBuilder {

    private String lastName = "DOE";
    private String firstNames = "Jane";
    private Civility civility = Civility.MADAME;
    private Sex sex = Sex.F;
    private LocalDate birthDate = LocalDate.of(1985, 7, 12);
    private String insuranceId;
    private String partnerId;
    private String labCode = "LAB-260505-001";
    private PatientId mergedInto;

    public static PatientBuilder aPatient() {
        return new PatientBuilder();
    }

    public PatientBuilder withName(String last, String first) {
        this.lastName = last;
        this.firstNames = first;
        return this;
    }

    public PatientBuilder withSex(Sex sex) {
        this.sex = sex;
        return this;
    }

    public PatientBuilder withCivility(Civility c) {
        this.civility = c;
        return this;
    }

    public PatientBuilder withBirthDate(LocalDate d) {
        this.birthDate = d;
        return this;
    }

    public PatientBuilder withInsuranceId(String id) {
        this.insuranceId = id;
        return this;
    }

    public PatientBuilder withPartnerId(String id) {
        this.partnerId = id;
        return this;
    }

    public PatientBuilder withLabCode(String s) {
        this.labCode = s;
        return this;
    }

    public PatientBuilder mergedInto(PatientId target) {
        this.mergedInto = target;
        return this;
    }

    public Patient build() {
        var p =
                Patient.create(
                        new NewPatient(
                                new PersonName(lastName, firstNames, null),
                                civility,
                                sex,
                                birthDate,
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
                        new LabCode(labCode));
        if (mergedInto != null) {
            p.mergeInto(mergedInto);
        }
        return p;
    }

    public CreatePatientRequest buildRequest() {
        return new CreatePatientRequest(
                new CreatePatientRequest.NameRequest(lastName, firstNames, null),
                civility,
                sex,
                birthDate,
                null,
                null,
                null,
                null,
                new CreatePatientRequest.ContactRequest(null, null),
                null,
                insuranceId,
                partnerId,
                null,
                null);
    }
}
